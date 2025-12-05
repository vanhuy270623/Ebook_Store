# Fix: Template Errors - December 4, 2025

**Date:** December 4, 2025  
**Issues Fixed:** 
1. Thymeleaf Template Processing Exception in 500 Error Page
2. Category Field Name Mismatch in User Book Templates

**Status:** ✅ Fixed

---

## Issue 1: 500 Error Page Template Bug

### Problem Description

The 500 error page was throwing a `TemplateProcessingException` when trying to render error details. The root cause was an incorrect SpringEL expression that attempted to use the `or` operator with non-boolean values.

### Error Message
```
org.springframework.expression.spel.SpelEvaluationException: EL1001E: Type conversion problem, 
cannot convert from java.util.Date to java.lang.Boolean
```

### Root Cause
On line 150 of `src/main/resources/templates/error/500.html`, the expression:
```html
th:if="${timestamp or path or error or message}"
```

This expression was attempting to use the `or` operator with variable names directly. When `timestamp` is a `java.util.Date` object, Spring EL tries to convert it to a Boolean for the OR operation, which fails.

### Solution

Changed all `th:if` expressions in the error details section to properly check for null values:

#### Before
```html
<div class="error-details" th:if="${timestamp or path or error or message}">
    <div th:if="${timestamp}">...</div>
    <div th:if="${path}">...</div>
    <div th:if="${error}">...</div>
    <div th:if="${message}">...</div>
</div>
```

#### After
```html
<div class="error-details" th:if="${timestamp != null or path != null or error != null or message != null}">
    <div th:if="${timestamp != null}">...</div>
    <div th:if="${path != null}">...</div>
    <div th:if="${error != null}">...</div>
    <div th:if="${message != null}">...</div>
</div>
```

---

## Issue 2: Category Field Name Mismatch

### Problem Description

User book list and category pages were throwing errors when trying to access category information. The templates were using `bookCategoryId` field name, but the `Category` entity uses `categoryId`.

### Error Message
```
org.springframework.expression.spel.SpelEvaluationException: EL1008E: 
Property or field 'bookCategoryId' cannot be found on object of type 'stu.datn.ebook_store.entity.Category'
```

### Root Cause

The application has TWO category entities:
1. **`Category`** entity with field `categoryId` - Used by user-facing controllers
2. **`BookCategory`** entity with field `bookCategoryId` - Used by admin controllers

Both map to the same `book_category` database table, but have different field names. The user book templates were incorrectly using `bookCategoryId` when they should use `categoryId`.

### Files Fixed

1. **`src/main/resources/templates/user/books/list.html`** (Lines 97, 99)
   - Changed: `cat.bookCategoryId` → `cat.categoryId`

2. **`src/main/resources/templates/user/books/category.html`** (Lines 96, 98)
   - Changed: `cat.bookCategoryId` → `cat.categoryId`

### Solution Summary

| Context | Entity Used | Field Name | Templates |
|---------|-------------|------------|-----------|
| User Pages | `Category` | `categoryId` | `user/books/list.html`, `user/books/category.html` |
| Admin Pages | `BookCategory` | `bookCategoryId` | `admin/books/form.html` |

---

## Technical Details

### Files Modified
- `src/main/resources/templates/error/500.html` - Lines 150-162
- `src/main/resources/templates/user/books/list.html` - Lines 97, 99
- `src/main/resources/templates/user/books/category.html` - Lines 96, 98

### Why These Fixes Work

#### Issue 1: Explicit Null Checks
1. **Explicit Null Checks:** Using `!= null` creates a proper boolean expression
2. **Type Safety:** No implicit type conversion is attempted
3. **Clear Intent:** The code clearly shows we're checking for existence of values

#### Issue 2: Correct Field Names
1. **Entity Awareness:** Templates now use the correct field names for their respective entities
2. **Consistency:** User templates use `Category.categoryId`, admin templates use `BookCategory.bookCategoryId`
3. **Type Safety:** SpringEL can now properly access the fields

### Thymeleaf/SpringEL Best Practices

In Thymeleaf with SpringEL expressions:
- ✅ **DO:** Use `${variable != null}` to check if a variable exists
- ✅ **DO:** Use `${variable != null and variable.property}` for chained checks
- ✅ **DO:** Verify entity field names match what templates expect
- ❌ **DON'T:** Use bare variables in boolean contexts: `${variable or otherVariable}`
- ❌ **DON'T:** Rely on implicit type conversion for non-String/Number types
- ❌ **DON'T:** Assume field names are the same across different entities

---

## Testing

After the fixes:
1. ✅ The 500 error page renders correctly
2. ✅ Error details section displays when relevant attributes are present
3. ✅ No more type conversion exceptions in logs
4. ✅ User book list page displays categories correctly
5. ✅ Category filtering works properly
6. ✅ Admin book form still works with BookCategory entity

---

## Related Files

### Error Page Fix
- `src/main/resources/templates/error/500.html` - Fixed
- `src/main/resources/templates/error/404.html` - Checked, no issues
- `src/main/resources/templates/error/403.html` - Checked, no issues

### Category Field Fix
- `src/main/resources/templates/user/books/list.html` - Fixed
- `src/main/resources/templates/user/books/category.html` - Fixed
- `src/main/resources/templates/admin/books/form.html` - Correct (uses BookCategory)
- `src/main/java/stu/datn/ebook_store/entity/Category.java` - Uses `categoryId`
- `src/main/java/stu/datn/ebook_store/entity/BookCategory.java` - Uses `bookCategoryId`

---

## Impact

### Issue 1
- **Severity:** Medium (error page was not displaying, causing cascading errors)
- **User Experience:** Improved - users now see proper error messages
- **System Stability:** Improved - no more error loops when displaying 500 errors

### Issue 2
- **Severity:** High (user book pages were completely broken)
- **User Experience:** Critical - users couldn't browse books by category
- **System Stability:** Improved - no more template processing exceptions

---

## Architecture Notes

### Dual Category Entities Issue

The application currently has two entities for the same database table:

```
book_category table
    ├── Category.java (categoryId)         → Used by: User Controllers
    └── BookCategory.java (bookCategoryId) → Used by: Admin Controllers
```

**Recommendation for Future:** Consider consolidating to a single entity to avoid confusion and maintenance issues. If keeping both:
1. Document clearly which entity is used where
2. Consider creating a mapper/adapter layer
3. Or use DTO pattern consistently across all layers

---

## Notes

### Issue 1
This type of bug can be particularly tricky because:
1. It only manifests when the error page itself is triggered
2. Creates a cascading error (error page throws error)
3. Can fill logs making real issues harder to find

Always test error pages independently to ensure they handle various attribute combinations gracefully.

### Issue 2
This bug highlights the importance of:
1. Consistent naming conventions across entities
2. Proper documentation of which entities are used where
3. Type-safe template binding (consider using DTOs)
4. Code review to catch entity/field name mismatches

---

## Lessons Learned

1. **Explicit is Better:** Always use explicit null checks in Thymeleaf expressions
2. **Entity Awareness:** Know which entity your controller passes to templates
3. **Field Name Verification:** When adding template code, verify field names against entity classes
4. **Avoid Duplication:** Having multiple entities for the same table creates maintenance overhead
5. **Test Error Pages:** Error pages need testing just like regular pages

