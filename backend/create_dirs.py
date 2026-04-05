import os

# Base path
base_path = r"c:\Users\Admin\OneDrive - MSFT\Desktop\STUDY BITS\25-26 SEM-2\1. ADBS\PROJECT\blood-donation-project-main\blood-donation-project\backend\src\main\java\com\G9\hemoconnect"

# Create directories
entity_path = os.path.join(base_path, "entity")
repository_path = os.path.join(base_path, "repository")

os.makedirs(entity_path, exist_ok=True)
os.makedirs(repository_path, exist_ok=True)

print("Directories created successfully:")
print(f"  - {entity_path}")
print(f"  - {repository_path}")
