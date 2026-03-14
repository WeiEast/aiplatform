package com.ext.config;

import com.ext.service.McpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP 工具初始化配置
 */
@Component
public class McpToolConfig implements CommandLineRunner {

    @Autowired
    private McpService mcpService;

    @Override
    public void run(String... args) throws Exception {
        // 注册天气查询工具
        registerWeatherTool();
        
        // 注册计算器工具
        registerCalculatorTool();
        
        // 注册时间工具
        registerTimeTool();
        
        System.out.println("MCP 工具已初始化");
    }

    /**
     * 注册天气查询工具
     */
    private void registerWeatherTool() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> cityProp = new HashMap<>();
        cityProp.put("type", "string");
        cityProp.put("description", "城市名称");
        properties.put("city", cityProp);
        schema.put("properties", properties);

        mcpService.registerTool("weather_query", "查询指定城市的天气", schema, arguments -> {
            String city = (String) arguments.get("city");
            return "模拟天气数据：" + city + "，晴，25°C，空气质量优";
        });
    }

    /**
     * 注册计算器工具
     */
    private void registerCalculatorTool() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> aProp = new HashMap<>();
        aProp.put("type", "number");
        aProp.put("description", "第一个数字");
        properties.put("a", aProp);
        
        Map<String, Object> bProp = new HashMap<>();
        bProp.put("type", "number");
        bProp.put("description", "第二个数字");
        properties.put("b", bProp);
        
        Map<String, Object> opProp = new HashMap<>();
        opProp.put("type", "string");
        opProp.put("description", "运算符：+, -, *, /");
        properties.put("operation", opProp);
        
        schema.put("properties", properties);

        mcpService.registerTool("calculator", "执行数学计算", schema, arguments -> {
            Double a = ((Number) arguments.get("a")).doubleValue();
            Double b = ((Number) arguments.get("b")).doubleValue();
            String operation = (String) arguments.get("operation");
            
            double result = switch (operation) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                case "/" -> b != 0 ? a / b : Double.NaN;
                default -> Double.NaN;
            };
            
            return "计算结果：" + a + " " + operation + " " + b + " = " + result;
        });
    }

    /**
     * 注册时间查询工具
     */
    private void registerTimeTool() {
        mcpService.registerTool("get_current_time", "获取当前日期和时间", arguments -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return "当前时间：" + now.format(formatter);
        });
    }
}
