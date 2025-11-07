//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package med.supply.system.model;

public class StorageItem {
    private final String sku;
    private final String name;
    private int quantity;

    public StorageItem(String sku, String name, int quantity) {
        if (sku != null && !sku.isBlank()) {
            if (name != null && !name.isBlank()) {
                if (quantity < 0) {
                    throw new IllegalArgumentException("Quantity must be non-negative");
                } else {
                    this.sku = sku.trim();
                    this.name = name.trim();
                    this.quantity = quantity;
                }
            } else {
                throw new IllegalArgumentException("Item name must not be blank");
            }
        } else {
            throw new IllegalArgumentException("SKU must not be blank");
        }
    }

    public String getSku() {
        return this.sku;
    }

    public String getName() {
        return this.name;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        } else {
            this.quantity = quantity;
        }
    }

    public String toString() {
        return "StorageItem{sku='" + this.sku + "', name='" + this.name + "', quantity=" + this.quantity + "}";
    }
}
 