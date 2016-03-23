package com.github.features;

import cucumber.api.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

/**
 * Created by david on 13/10/15.
 */
@RunWith(CucumberWithSerenity.class)
@CucumberOptions(features = "src/test/resources/com/github/features/b")
public class SerenityBug2Test {
}
