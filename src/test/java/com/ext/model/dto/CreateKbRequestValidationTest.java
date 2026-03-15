package com.ext.model.dto;

import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CreateKbRequestValidationTest {
    
    private final Validator validator;
    
    public CreateKbRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }
    
    @Test
    public void testValidRequest() {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("有效名称");
        request.setDescription("有效的描述");
        request.setBusinessLine("test");
        
        Set<ConstraintViolation<CreateKbRequest>> violations = validator.validate(request);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    public void testEmptyNameShouldFail() {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("");
        
        Set<ConstraintViolation<CreateKbRequest>> violations = validator.validate(request);
        
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("name", violations.iterator().next().getPropertyPath().toString());
    }
    
    @Test
    public void testNameTooLong() {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("a".repeat(129)); // 超过 128 字符限制
        
        Set<ConstraintViolation<CreateKbRequest>> violations = validator.validate(request);
        
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
    }
    
    @Test
    public void testDescriptionTooLong() {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("有效名称");
        request.setDescription("a".repeat(513)); // 超过 512 字符限制
        
        Set<ConstraintViolation<CreateKbRequest>> violations = validator.validate(request);
        
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
    }
    
    @Test
    public void testBusinessLineTooLong() {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("有效名称");
        request.setBusinessLine("a".repeat(65)); // 超过 64 字符限制
        
        Set<ConstraintViolation<CreateKbRequest>> violations = validator.validate(request);
        
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
    }
}
