package com.ext.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson 配置测试
 * 验证 JSON 序列化/反序列化配置
 */
@DisplayName("Jackson 配置测试")
@SpringBootTest
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("ObjectMapper Bean - 不为空")
    void testObjectMapperBeanExists() {
        assertNotNull(objectMapper, "ObjectMapper 应该被正确注入");
    }

    @Test
    @DisplayName("FAIL_ON_UNKNOWN_PROPERTIES - 已禁用")
    void testFailOnUnknownPropertiesDisabled() {
        // 验证配置是否正确应用
        assertFalse(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES),
                "应该禁用 FAIL_ON_UNKNOWN_PROPERTIES 特性");
    }

    @Test
    @DisplayName("解析包含未知字段的 JSON - 不抛异常")
    void testParseJsonWithUnknownFields() throws Exception {
        String json = "{\"name\":\"测试\",\"unknownField\":\"未知值\",\"anotherField\":123}";
        
        // 应该不会抛出异常
        assertDoesNotThrow(() -> {
            var node = objectMapper.readTree(json);
            assertEquals("测试", node.get("name").asText());
        });
    }

    @Test
    @DisplayName("序列化对象 - 不包含 null 值")
    void testSerializeWithNonNullNull() throws Exception {
        TestObject obj = new TestObject();
        obj.name = "测试";
        obj.value = null;

        String json = objectMapper.writeValueAsString(obj);
        
        // 验证不包含 null 字段
        assertTrue(json.contains("name"), "应该包含 name 字段");
        assertFalse(json.contains("value"), "不应该包含 value 字段（因为是 null）");
    }

    @Test
    @DisplayName("解析嵌套 JSON")
    void testParseNestedJson() throws Exception {
        String json = """
            {
                "agent": {
                    "id": "test-123",
                    "name": "测试智能体",
                    "config": {
                        "enabled": true,
                        "priority": 1
                    }
                }
            }
            """;

        var root = objectMapper.readTree(json);
        var agent = root.get("agent");
        
        assertNotNull(agent);
        assertEquals("test-123", agent.get("id").asText());
        assertEquals("测试智能体", agent.get("name").asText());
        assertTrue(agent.get("config").get("enabled").asBoolean());
        assertEquals(1, agent.get("config").get("priority").asInt());
    }

    // 测试用的内部类
    static class TestObject {
        public String name;
        public String value;
    }
}
