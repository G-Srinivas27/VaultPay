package com.srinivas.vaultpay.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * The implementation that powers the {@link PositiveAmount} annotation.
 *
 * <p><b>ConstraintValidator&lt;A, T&gt;:</b>
 * <ul>
 *   <li>A = the annotation type it validates ({@link PositiveAmount})</li>
 *   <li>T = the type of value it validates ({@link BigDecimal})</li>
 * </ul>
 *
 * <p><b>Why does isValid() return true for null?</b>
 * Bean Validation convention: null values are considered valid by constraint validators.
 * The {@code @NotNull} annotation is responsible for rejecting nulls.
 * This separation of concerns means:
 * <ul>
 *   <li>@NotNull → "value must be present"</li>
 *   <li>@PositiveAmount → "if present, value must be > 0"</li>
 * </ul>
 * All standard validators (@Min, @DecimalMin, @Size, etc.) follow this same convention.
 *
 * <p><b>Why compareTo() and not > operator?</b>
 * BigDecimal cannot use > or == operators (those compare object references).
 * {@code compareTo()} returns:
 * <ul>
 *   <li>-1 if the value is less than the argument</li>
 *   <li>0 if equal</li>
 *   <li>1 if greater than the argument</li>
 * </ul>
 * So {@code value.compareTo(BigDecimal.ZERO) > 0} means: value is greater than zero.
 */
public class PositiveAmountValidator implements ConstraintValidator<PositiveAmount, BigDecimal> {

    /**
     * Called once after the validator is instantiated.
     * We have no configuration to read from the annotation, so we leave this empty.
     */
    @Override
    public void initialize(PositiveAmount constraintAnnotation) {
        // Nothing to configure
    }

    /**
     * Core validation logic — called for every annotated field during request validation.
     *
     * @param value   the BigDecimal value to validate (may be null)
     * @param context provides ways to build custom violation messages
     * @return true if valid (null or > 0), false if invalid (== 0 or negative)
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // null is valid here — let @NotNull handle the null case separately
        if (value == null) {
            return true;
        }
        // compareTo returns > 0 if value is greater than ZERO
        return value.compareTo(BigDecimal.ZERO) > 0;
    }
}
