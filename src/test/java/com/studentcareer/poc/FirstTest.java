
package com.studentcareer.poc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FirstTest {

    @Test
    void shouldAddTwoNumbers() {
        // Arrange
        int first = 5;
        int second = 3;

        // Act
        int result = first + second;

        // Assert
        assertEquals(8, result);
    }
}
