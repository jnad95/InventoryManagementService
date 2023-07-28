import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


/**
 * User orders a product with id (1234, cricket-bat, 1)
 * System checks if the item with given specs is avaiable.
 * <p>
 * if the item is available
 * - an orderId against against the product selected is returned.
 * - it is blocked for 5 mins for user to complete the payment.
 * else item unavailable message is returned.
 * <p>
 * <p>
 * if the payment against the order Id is successful then release hold and remove inventory
 * else if payment is failed then release hold and dont remove inventory
 * order could fail due to following reasons-
 * - 5 mins threshold expired.
 * - or payment declined by merchant, etc.
 */

public class Solution {
    public static void main(String args[]) throws Exception {
        Inventory inventory = new Inventory();
        Orders orders = new Orders();
        InventoryManager manager = new InventoryManagerImpl(inventory, orders);

        /* ****** Test Data Creation ****** */
        manager.createProduct("1234", "Cricket bat", 40);
        manager.createProduct("2345", "Keyboard", 20);
        manager.createProduct("3456", "Mouse", 10);
        manager.createProduct("5677", "Eraser", 50);

        String orderId1 = String.valueOf(Math.floor(Math.random() * 10000));
        String orderId2 = String.valueOf(Math.floor(Math.random() * 10000));
        String orderId3 = String.valueOf(Math.floor(Math.random() * 10000));
        String orderId4 = String.valueOf(Math.floor(Math.random() * 10000));
        /* **** Test Data Creation Ends ****** */

        System.out.println(manager.getInventory("1234"));

        boolean blockedStatus = manager.blockInventory("1234", 25, orderId1);
        System.out.println("blockedStatus: " + blockedStatus);

        blockedStatus = manager.blockInventory("1234", 30, orderId2);
        System.out.println("blockedStatus: " + blockedStatus);

//        boolean confirmOrderStatus = manager.confirmOrder(orderId1);
//        System.out.println("confirmOrderStatus: " + confirmOrderStatus);
//
//        confirmOrderStatus = manager.confirmOrder(orderId2);
//        System.out.println("confirmOrderStatus: " + confirmOrderStatus);

        System.out.println(manager.getInventory("1234"));
    }
}

class Product {

    private final String productId;
    private final String name;
    private int count;
    private int availableCount;

    public Product(String productId, String name, int count) {
        this.productId = productId;
        this.name = name;
        this.count = count;
        this.availableCount = count;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getAvailableCount() {
        return availableCount;
    }

    public void addCount(int count) {
        this.count += count;
    }

    public boolean blockCount(int blockedCount) {
        if (blockedCount > 0 && count >= blockedCount && availableCount >= blockedCount) {
            availableCount = count - blockedCount;
            return true;
        } else {
            System.out.println("Inventory cant be blocked");
            return false;
        }
    }

    public boolean releaseCount(int releaseCount) {
        if (releaseCount > 0 && count >= releaseCount && (count - availableCount) >= releaseCount) {
            count = count - releaseCount;
            return true;
        } else {
            System.out.println("Inventory cant be blocked");
            return false;
        }
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId='" + productId + '\'' +
                ", Name='" + name + '\'' +
                ", count=" + availableCount +
                '}';
    }
}

class Inventory {

    // ProductId to Item Map
    private final Map<String, Product> productList;

    public Inventory() {
        this.productList = new HashMap<>();
    }

    public Map<String, Product> getProductList() {
        return productList;
    }

    public boolean addInventory(String productId, Product product) {
        // Assuming productId is unique
        if (productList.containsKey(productId)) {
            System.out.println("Duplicate Product is being created");
            return false;
        } else {
            productList.put(productId, product);
            return true;
        }
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "productList=" + productList +
                '}';
    }
}

class Order {

    private final String orderId;
    private final String productId;
    int count;

    public Order(String orderId, String productId, int count) {
        this.orderId = orderId;
        this.productId = productId;
        this.count = count;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", productId='" + productId + '\'' +
                ", count=" + count +
                '}';
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public int getCount() {
        return count;
    }
}

class Orders {

    private final Map<String, Order> orders;

    public Orders() {
        this.orders = new HashMap<>();
    }

    public Map<String, Order> getOrderList() {
        return orders;
    }
}

interface InventoryManager {

    /**
     * creates a product with given
     *
     * @param productId,
     * @param name,
     * @param count
     */
    Boolean createProduct(String productId, String name, int count);

    /**
     * fetches the inventory against a productId
     *
     * @param productId,
     */
    int getInventory(String productId);

    /**
     * blocks the inventory and creates order corresponding to it.
     *
     * @param productId,
     * @param count,
     * @param orderId
     * @return true if order is created successfully false otherwise
     */
    boolean blockInventory(String productId, int count, String orderId);

    /**
     * confirms an order against given orderId. This internally release inventory against the same orderId
     *
     * @param orderId
     * @return true if order if fulfilled false otherwise
     */
    boolean confirmOrder(String orderId);
}

class InventoryManagerImpl implements InventoryManager {

    private Inventory inventory;
    private InventoryLocker inventoryLocker;
    private Orders orders;

    public InventoryManagerImpl(Inventory inventory, Orders orders) {
        this.inventory = inventory;
        this.orders = orders;
        this.inventoryLocker = new InventoryLockerImpl(inventory);
    }

    @Override
    public Boolean createProduct(String productId, String name, int count) {
        return inventory.addInventory(productId, new Product(productId, name, count));

    }

    @Override
    public int getInventory(String productId) {
        return inventory.getProductList().get(productId).getAvailableCount();
    }

    @Override
    public boolean blockInventory(String productId, int count, String orderId) {
        Order order = new Order(orderId, productId, count);
        orders.getOrderList().put(order.getOrderId(), order);
        return inventoryLocker.lock(order);

    }

    @Override
    public boolean confirmOrder(String orderId) {
        return inventoryLocker.release(orders.getOrderList().get(orderId));
    }
}

interface InventoryLocker {

    /**
     * locks the inventory against an order
     * returns boolean if the operation was successful false otherwise
     */
    boolean lock(Order order);

    /**
     * release the inventory blocked against an order
     * returns boolean if the operation was successful false otherwise
     */
    boolean release(Order order);
}

class InventoryLockerImpl implements InventoryLocker {

    private final Inventory inventory;

    InventoryLockerImpl(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public boolean lock(Order order) {

        Date startTime = new Date(System.currentTimeMillis());
        Date expiryTime = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));

        Product product = inventory.getProductList().get(order.getProductId());
        return product.blockCount(order.getCount());
    }

    @Override
    public boolean release(Order order) {
        Product product = inventory.getProductList().get(order.getProductId());
        return product.releaseCount(order.getCount());
    }
}

