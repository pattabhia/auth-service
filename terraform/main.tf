terraform {
  required_version = ">= 1.5"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.80"
    }
  }

  backend "azurerm" {
    resource_group_name  = "hai-indexer-tfstate-rg"
    storage_account_name = "haiindexertfstate"
    container_name       = "tfstate"
    key                  = "auth-service.tfstate"
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = false
    }
  }
}

# Variables
variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "location" {
  description = "Azure region"
  type        = string
  default     = "eastus"
}

variable "resource_group_name" {
  description = "Resource group name"
  type        = string
  default     = "hai-indexer-rg"
}

# Resource Group
resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name
  location = var.location

  tags = {
    Environment = var.environment
    Service     = "auth-service"
    ManagedBy   = "terraform"
  }
}

# Azure Kubernetes Service (AKS)
resource "azurerm_kubernetes_cluster" "main" {
  name                = "hai-indexer-aks-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  dns_prefix          = "hai-indexer-${var.environment}"

  default_node_pool {
    name       = "default"
    node_count = 3
    vm_size    = "Standard_D2s_v3"

    upgrade_settings {
      max_surge = "10%"
    }
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin = "azure"
    service_cidr   = "10.0.0.0/16"
    dns_service_ip = "10.0.0.10"
  }

  # Enable Istio service mesh
  service_mesh_profile {
    mode      = "Istio"
    revisions = ["asm-1-17"]
  }

  tags = {
    Environment = var.environment
    Service     = "auth-service"
  }
}

# Azure Redis Cache (for token revocation)
resource "azurerm_redis_cache" "main" {
  name                = "hai-indexer-redis-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  capacity            = 1
  family              = "C"
  sku_name            = "Standard"

  enable_non_ssl_port = false
  minimum_tls_version = "1.2"

  redis_configuration {
    maxmemory_policy = "allkeys-lru"
  }

  tags = {
    Environment = var.environment
    Service     = "auth-service"
  }
}

# Azure Key Vault (for secrets)
resource "azurerm_key_vault" "main" {
  name                = "hai-intel-kv-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "standard"

  purge_protection_enabled = true

  network_acls {
    default_action = "Deny"
    bypass         = "AzureServices"
  }

  tags = {
    Environment = var.environment
    Service     = "auth-service"
  }
}

data "azurerm_client_config" "current" {}

# Grant AKS access to Key Vault
resource "azurerm_key_vault_access_policy" "aks" {
  key_vault_id = azurerm_key_vault.main.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_kubernetes_cluster.main.kubelet_identity[0].object_id

  secret_permissions = [
    "Get",
    "List"
  ]
}

# Azure Log Analytics (for audit logs)
resource "azurerm_log_analytics_workspace" "main" {
  name                = "hai-indexer-logs-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "PerGB2018"
  retention_in_days   = 90 # ✅ P0 FIX: 90-day retention for compliance

  tags = {
    Environment = var.environment
    Service     = "auth-service"
  }
}

# Azure Container Registry
resource "azurerm_container_registry" "main" {
  name                = "haiintel-acr${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "Standard"
  admin_enabled       = false

  tags = {
    Environment = var.environment
    Service     = "auth-service"
  }
}

# Grant AKS pull access to ACR
resource "azurerm_role_assignment" "aks_acr_pull" {
  principal_id                     = azurerm_kubernetes_cluster.main.kubelet_identity[0].object_id
  role_definition_name             = "AcrPull"
  scope                            = azurerm_container_registry.main.id
  skip_service_principal_aad_check = true
}

# Outputs
output "aks_cluster_name" {
  value = azurerm_kubernetes_cluster.main.name
}

output "aks_cluster_id" {
  value = azurerm_kubernetes_cluster.main.id
}

output "redis_hostname" {
  value     = azurerm_redis_cache.main.hostname
  sensitive = true
}

output "redis_primary_key" {
  value     = azurerm_redis_cache.main.primary_access_key
  sensitive = true
}

output "key_vault_uri" {
  value = azurerm_key_vault.main.vault_uri
}

output "container_registry_login_server" {
  value = azurerm_container_registry.main.login_server
}

output "log_analytics_workspace_id" {
  value = azurerm_log_analytics_workspace.main.id
}

