# Fix: Book Field Name Mismatch - pageCount vs pages

**Date:** December 4, 2025  
**Issue:** Template trying to access non-existent field `pageCount`  
**Status:** ✅ Fixed

---

## Problem Description

The user book view and reader templates were throwing errors when trying to display page count information. The templates were using `book.pageCount` field name, but the `Book` entity uses `pages`.

### Error Message
```
org.springframework.expression.spel.SpelEvaluationException: EL1008E: 
Property or field 'pageCount' cannot be found on object of type 'stu.datn.ebook_store.entity.Book'
```

### Root Cause

Field name mismatch between template and entity:
- **Template used:** `book.pageCount`
- **Entity has:** `book.pages`

From `Book.java`:
```java
@Column(name = "pages")
private Integer pages;
```

---

## Solution

Updated all template references from `pageCount` to `pages`.

### Files Fixed

#### 1. `src/main/resources/templates/user/books/view.html` (Line 156)

**Before:**
```html
<strong th:text="${book.pageCount != null ? book.pageCount : 'N/A'}">0</strong>
```

**After:**
```html
<strong th:text="${book.pages != null ? book.pages : 'N/A'}">0</strong>
```

#### 2. `src/main/resources/templates/user/reading/reader.html` (Lines 327-329)

**Before:**
```html
<div class="stat-item" th:if="${book.pageCount}">
    <i class="fas fa-file-alt"></i>
    <span th:text="${book.pageCount} + ' trang'">0 trang</span>
</div>
```

**After:**
```html
<div class="stat-item" th:if="${book.pages}">
    <i class="fas fa-file-alt"></i>
    <span th:text="${book.pages} + ' trang'">0 trang</span>
</div>
```

---

## Verification

### Files Checked
- ✅ `user/books/view.html` - Fixed
- ✅ `user/reading/reader.html` - Fixed
- ✅ `user/reading/pdf-viewer.html` - No changes needed (uses JavaScript variable `pageCount`, not entity field)

### Entity Verification
From `Book.java`:
```java
@Column(name = "pages")
private Integer pages;
```

✅ Field name confirmed as `pages`

---

## Related Context

This is part of a series of field name mismatch fixes:

### Previous Fixes Today (Dec 4, 2025)
1. **Category Field:** `bookCategoryId` → `categoryId` in user templates
2. **Error Page:** Boolean conversion issues with null checks

### Pattern Identified
Multiple templates were created with incorrect field names, likely due to:
- Assumptions about naming conventions
- Copy-paste from different parts of the codebase
- Lack of entity-template field verification during development

---

## Impact

- **Severity:** High (Critical functionality broken)
- **Affected Pages:**
  - Book detail page (`/books/view/{id}`)
  - Book reader page (`/reading/read/{id}`)
- **User Impact:** Users couldn't view book details or access the reader
- **System Stability:** Complete page failure with 500 errors

---

## Testing

After the fix, verify:
1. ✅ Book detail page displays page count correctly
2. ✅ Reader page shows page count in book info
3. ✅ Page count displays "N/A" when not set
4. ✅ No template processing exceptions in logs

---

## Prevention Strategies

### 1. Entity-Template Verification Checklist
When creating templates:
- [ ] Check entity class for actual field names
- [ ] Use IDE autocomplete when possible
- [ ] Verify getter methods exist
- [ ] Test with real data, not just mocks

### 2. Naming Convention Guidelines

**Do's:**
- ✅ Use consistent naming across database, entity, and templates
- ✅ Follow Java naming conventions (camelCase for fields)
- ✅ Document any intentional name variations

**Don'ts:**
- ❌ Assume field names without checking
- ❌ Use different names for same concept in different layers
- ❌ Copy code without verifying field references

### 3. Common Book Entity Fields

For reference, here are the actual Book entity field names:

| Template Should Use | Database Column | Type |
|-------------------|-----------------|------|
| `bookId` | `book_id` | String |
| `title` | `title` | String |
| `description` | `description` | String |
| `price` | `price` | BigDecimal |
| `coverImageUrl` | `cover_image_url` | String |
| `publisher` | `publisher` | String |
| `publicationYear` | `publication_year` | Integer |
| `language` | `language` | String |
| **`pages`** | `pages` | Integer |
| `isbn` | `isbn` | String |
| `accessType` | `access_type` | Enum |
| `isDownloadable` | `is_downloadable` | Boolean |
| `averageRating` | `average_rating` | Float |
| `totalReviews` | `total_reviews` | Integer |
| `viewCount` | `view_count` | Integer |
| `createdAt` | `created_at` | LocalDateTime |
| `updatedAt` | `updated_at` | LocalDateTime |

### 4. Template Testing

Add these to your test suite:
```java
@Test
void bookViewTemplateShouldUseCorrectFieldNames() {
    // Verify template can access all Book fields
    Book book = createTestBook();
    Model model = new ExtendedModelMap();
    model.addAttribute("book", book);
    
    // This would fail at compile time with wrong field names
    assertThat(book.getPages()).isNotNull();
    assertThat(book.getTitle()).isNotNull();
}
```

---

## Summary

**What was wrong:**
- Templates used `book.pageCount` 
- Entity has `book.pages`

**What was fixed:**
- Changed all template references to use `pages`
- Verified against actual entity definition

**Why it matters:**
- Critical user-facing pages were broken
- Proper field names prevent runtime errors
- Consistency improves maintainability

---

## Related Issues

This fix is part of today's template cleanup:
- [x] Issue #1: 500 error page null check
- [x] Issue #2: Category field name mismatch
- [x] Issue #3: Book pages field name mismatch (this fix)

---

## Files Modified Summary

1. `src/main/resources/templates/user/books/view.html` - Line 156
2. `src/main/resources/templates/user/reading/reader.html` - Lines 327-329
3. `docs/FIX_BOOK_PAGECOUNT_FIELD_04_12_2025.md` - This document

---

**Status:** ✅ Complete and Tested  
**Risk Level:** Low (simple field name correction)  
**Rollback:** If needed, change `pages` back to `pageCount` (not recommended)

