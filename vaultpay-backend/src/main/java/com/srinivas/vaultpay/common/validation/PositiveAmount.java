package com.srinivas.vaultpay.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom constraint annotation — validates that a {@link java.math.BigDecimal}
 * field represents a strictly positive financial amount (greater than zero).
 *
 * <p><b>Why a custom annotation instead of @DecimalMin?</b>
 * <pre>
 *   // Before — every DTO repeated this:
 *   @NotNull(message = "Amount is required")
 *   @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
 *   BigDecimal amount;
 *
 *   // After — single, reusable, self-documenting:
 *   @PositiveAmount
 *   BigDecimal amount;
 * </pre>
 *
 * Benefits:
 * <ul>
 *   <li><b>DRY</b> — one place to define the rule, applied everywhere</li>
 *   <li><b>Readable</b> — @PositiveAmount reads like English, intent is clear</li>
 *   <li><b>Maintainable</b> — change validation logic in ONE place, not 3</li>
 *   <li><b>Composable</b> — multiple constraints in one annotation</li>
 * </ul>
 *
 * <p><b>How custom annotations work:</b>
 * <ul>
 *   <li>{@code @Constraint(validatedBy = ...)} — links to the validator implementation</li>
 *   <li>{@code @Target} — where the annotation can be placed (fields and parameters)</li>
 *   <li>{@code @Retention(RUNTIME)} — annotation is available at runtime for reflection</li>
 *   <li>{@code @Documented} — shows up in generated Javadoc</li>
 * </ul>
 *
 * <p>The {@code message}, {@code groups}, and {@code payload} methods are
 * REQUIRED by the Bean Validation spec for every constraint annotation.
 */
@Documented
@Constraint(validatedBy = PositiveAmountValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PositiveAmount {

    String message() default "Amount must be a positive value greater than zero";

    // groups() and payload() are required by Bean Validation spec
    // They enable grouping and severity metadata — we leave as defaults
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
