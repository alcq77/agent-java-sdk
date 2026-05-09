package io.github.alcq77.cqagent.plugin.time;

import io.github.alcq77.cqagent.spi.tool.ProductTool;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Example plugin that returns the current business date.
 */
public class BusinessCalendarTool implements ProductTool {

    @Override
    public String name() {
        return "business_calendar";
    }

    @Override
    public boolean supports(String userInput) {
        if (userInput == null) {
            return false;
        }
        String text = userInput.toLowerCase();
        return text.contains("business date");
    }

    @Override
    public String execute(String userInput) {
        return "today business date: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
