import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SauceDemoTests {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    void setUp() throws InterruptedException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-infobars");
        options.addArguments("--start-maximized");
        options.addArguments("--incognito");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @BeforeEach
    void loginAndClearCart() throws InterruptedException {
        login();
    }

    void login() throws InterruptedException {
        driver.get("https://www.saucedemo.com/");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("user-name"))).sendKeys("standard_user");
        driver.findElement(By.id("password")).sendKeys("secret_sauce");
        driver.findElement(By.id("login-button")).click();
        wait.until(ExpectedConditions.urlContains("inventory"));

    }



    @AfterEach
    void logout() {
        try {
            driver.findElement(By.id("react-burger-menu-btn")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("logout_sidebar_link"))).click();
        } catch (Exception ignored) {
        }

    }

    @AfterAll
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void testSuccessfulLogin() throws InterruptedException {
        Assertions.assertTrue(driver.getCurrentUrl().contains("inventory"));
        Thread.sleep(1000);
    }

    @Test
    void testInvalidPassword() throws InterruptedException {
        logout(); // выйти, чтобы протестировать вход с неправильным паролем
        driver.get("https://www.saucedemo.com/");
        driver.findElement(By.id("user-name")).sendKeys("standard_user");
        driver.findElement(By.id("password")).sendKeys("wrong_pass");
        driver.findElement(By.id("login-button")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='error']")));
        Assertions.assertTrue(error.getText().contains("Username and password do not match"));
        Thread.sleep(1000);
    }

    @Test
    void testEmptyCredentials() throws InterruptedException {
        logout();
        driver.get("https://www.saucedemo.com/");
        driver.findElement(By.id("login-button")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='error']")));
        Assertions.assertTrue(error.getText().contains("Username is required"));
        Thread.sleep(1000);
    }

    @Test
    void testLockedOutUser() throws InterruptedException {
        logout();
        driver.get("https://www.saucedemo.com/");
        driver.findElement(By.id("user-name")).sendKeys("locked_out_user");
        driver.findElement(By.id("password")).sendKeys("secret_sauce");
        driver.findElement(By.id("login-button")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='error']")));
        Assertions.assertTrue(error.getText().contains("locked out"));
        Thread.sleep(1000);
    }

    @Test
    void testLogout() throws InterruptedException {
        Assertions.assertTrue(driver.findElement(By.className("app_logo")).isDisplayed());
        Thread.sleep(1000);
    }

    @Test
    void testItemsVisible() throws InterruptedException {
        List<WebElement> items = driver.findElements(By.className("inventory_item"));
        Assertions.assertFalse(items.isEmpty());
        Thread.sleep(1000);
    }


    @Test
    void testAddAllItemsToCart() throws InterruptedException {
        List<WebElement> buttons = driver.findElements(By.cssSelector(".inventory_item button"));
        for (WebElement btn : buttons) {
            btn.click();
        }
        WebElement badge = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("shopping_cart_badge")));
        Assertions.assertEquals("6", badge.getText());
        Thread.sleep(1000);

        driver.get("https://www.saucedemo.com/cart.html");

        // Ждём появления кнопок "Remove", если корзина не пуста
        List<WebElement> removeButtons = driver.findElements(By.xpath("//button[text()='Remove']"));

        for (WebElement button : removeButtons) {
            try {
                button.click();
            } catch (StaleElementReferenceException e) {
                // Повторяем поиск элемента, если DOM обновился после клика
                WebElement retryButton = driver.findElement(By.xpath("//button[text()='Remove']"));
                retryButton.click();
            }
        }
    }



    @Test
    void testCheckCartItem() throws InterruptedException {
        driver.findElement(By.cssSelector(".inventory_item button")).click();
        driver.findElement(By.className("shopping_cart_link")).click();
        WebElement item = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("inventory_item_name")));
        Assertions.assertTrue(item.isDisplayed());
        Thread.sleep(1000);
    }

    @Test
    void testSortZToA() {
        // Выбираем сортировку через Select (а не sendKeys)
        WebElement sortDropdown = driver.findElement(By.className("product_sort_container"));
        Select select = new Select(sortDropdown);
        select.selectByVisibleText("Name (Z to A)");

        // Ждём, пока список обновится
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.className("inventory_item_name")));

        // Получаем названия товаров
        List<WebElement> nameElements = driver.findElements(By.className("inventory_item_name"));
        List<String> actualNames = nameElements.stream().map(WebElement::getText).toList();

        // Делаем копию и сортируем вручную
        List<String> expectedNames = new ArrayList<>(actualNames);
        expectedNames.sort(Comparator.reverseOrder());

        // Проверяем, что список отсортирован от Z к A
        Assertions.assertEquals(expectedNames, actualNames);
    }


    @Test
    void testBeginCheckout() throws InterruptedException {
        driver.findElement(By.cssSelector(".inventory_item button")).click();
        driver.findElement(By.className("shopping_cart_link")).click();
        driver.findElement(By.id("checkout")).click();
        Assertions.assertTrue(driver.getCurrentUrl().contains("checkout-step-one"));
        Thread.sleep(1000);
    }

    @Test
    void testFillCheckoutForm() throws InterruptedException {
        driver.findElement(By.cssSelector(".inventory_item button")).click();
        driver.findElement(By.className("shopping_cart_link")).click();
        driver.findElement(By.id("checkout")).click();
        driver.findElement(By.id("first-name")).sendKeys("John");
        driver.findElement(By.id("last-name")).sendKeys("Doe");
        driver.findElement(By.id("postal-code")).sendKeys("12345");
        driver.findElement(By.id("continue")).click();
        Assertions.assertTrue(driver.getCurrentUrl().contains("checkout-step-two"));
        Thread.sleep(1000);
    }

}