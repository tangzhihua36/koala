package org.openkoala.opencis.jenkins.configureImpl.user;

import org.openkoala.opencis.api.Developer;
import org.openkoala.opencis.jenkins.util.SeleniumUtil;
import org.openkoala.opencis.jenkins.util.UrlUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.util.concurrent.TimeUnit;

/**
 * User: zjzhai
 * Date: 1/8/14
 * Time: 2:48 PM
 */
public class UserCreator {
    private String jenkinsUrl;

    private String error;


    public UserCreator(String jenkinsUrl) {
        this.jenkinsUrl = UrlUtil.removeEndIfExists(jenkinsUrl, "/");
    }

    public static UserCreator newInstance(String jenkinsUrl) {
        return new UserCreator(jenkinsUrl);
    }


    public boolean createUser(Developer developer, WebDriver driver) {
        driver.get(jenkinsUrl + "/securityRealm/addUser");

        WebElement usernameInput = driver.findElement(By.name("username"));
        usernameInput.sendKeys(developer.getName());
        WebElement passwordInput1 = driver.findElement(By.name("password1"));
        passwordInput1.sendKeys(developer.getPassword());

        WebElement passwordInput2 = driver.findElement(By.name("password2"));
        passwordInput2.sendKeys(developer.getPassword());

        WebElement fullnameInput = driver.findElement(By.name("fullname"));
        fullnameInput.sendKeys(developer.getFullName());

        WebElement emailInput = driver.findElement(By.name("email"));
        emailInput.sendKeys(developer.getEmail());

        emailInput.submit();

        if (!driver.getCurrentUrl().endsWith("securityRealm/")) {
            error = driver.findElement(By.cssSelector("div.error")).getText();
            driver.quit();
            return false;
        }
        driver.quit();
        return true;

    }

    public String getError() {
        return error;
    }
}