# Frontend Structure - Ebook Store

## Tổng Quan Frontend

Frontend của Ebook Store được xây dựng với **Thymeleaf Template Engine**, tích hợp **Bootstrap** cho responsive design và các template components từ AdminLTE (cho admin) và template người dùng tùy chỉnh.

---

## Cấu Trúc Frontend

```
src/main/resources/
├── static/                    # Static resources
│   ├── admin_template/       # Admin UI assets
│   │   ├── css/
│   │   ├── js/
│   │   ├── images/
│   │   ├── fonts/
│   │   └── plugins/
│   ├── user_template/        # User UI assets
│   │   ├── css/
│   │   ├── js/
│   │   ├── images/
│   │   └── scss/
│   └── shared/               # Shared resources
│       ├── css/
│       ├── js/
│       └── images/
└── templates/                # Thymeleaf templates
    ├── home.html            # Public homepage
    ├── admin/               # Admin pages
    │   ├── dashboard.html
    │   ├── books/
    │   │   ├── list.html
    │   │   ├── create.html
    │   │   ├── edit.html
    │   │   └── detail.html
    │   ├── users/
    │   │   ├── list.html
    │   │   ├── create.html
    │   │   └── edit.html
    │   └── layout/
    │       ├── header.html
    │       ├── sidebar.html
    │       └── footer.html
    ├── user/                # User pages
    │   ├── index.html
    │   ├── library.html
    │   ├── profile.html
    │   ├── cart.html
    │   ├── orders.html
    │   └── layout/
    │       ├── header.html
    │       ├── footer.html
    │       └── sidebar.html
    ├── auth/                # Authentication pages
    │   ├── login.html
    │   └── register.html
    └── error/               # Error pages
        ├── 404.html
        ├── 403.html
        └── 500.html
```

---

## Template Engine - Thymeleaf

### Thymeleaf Basics

**Namespace**:
```html
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

**Variable Expression**: `${...}`
```html
<h1 th:text="${book.title}">Book Title</h1>
```

**URL Expression**: `@{...}`
```html
<a th:href="@{/books/{id}(id=${book.id})}">View Book</a>
```

**Selection Expression**: `*{...}` (with th:object)
```html
<form th:object="${book}">
    <input type="text" th:field="*{title}"/>
</form>
```

**Message Expression**: `#{...}` (i18n)
```html
<p th:text="#{welcome.message}">Welcome</p>
```

---

## Layout Structure

### Admin Layout

**Base Template**: `admin/layout/base.html` (if exists)

**Components**:
1. **Header** (`admin/layout/header.html`)
   - Logo
   - Search bar
   - User dropdown
   - Notifications

2. **Sidebar** (`admin/layout/sidebar.html`)
   - Navigation menu
   - Dashboard
   - Books management
   - Users management
   - Orders
   - Reports

3. **Footer** (`admin/layout/footer.html`)
   - Copyright
   - Version info

**Example Admin Page**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Admin Dashboard</title>
    <link rel="stylesheet" th:href="@{/admin_template/css/adminlte.min.css}"/>
</head>
<body class="hold-transition sidebar-mini">
    <div class="wrapper">
        <!-- Header -->
        <div th:replace="admin/layout/header :: header"></div>
        
        <!-- Sidebar -->
        <div th:replace="admin/layout/sidebar :: sidebar"></div>
        
        <!-- Content -->
        <div class="content-wrapper">
            <section class="content">
                <!-- Page content here -->
            </section>
        </div>
        
        <!-- Footer -->
        <div th:replace="admin/layout/footer :: footer"></div>
    </div>
    
    <script th:src="@{/admin_template/js/adminlte.min.js}"></script>
</body>
</html>
```

---

### User Layout

**Base Template**: `user/layout/base.html` (if exists)

**Components**:
1. **Header** (`user/layout/header.html`)
   - Logo
   - Navigation (Home, Categories, My Library)
   - Search bar
   - Cart icon
   - User menu / Login button

2. **Footer** (`user/layout/footer.html`)
   - About
   - Contact
   - Social links
   - Copyright

**Example User Page**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <title th:text="${pageTitle}">Ebook Store</title>
    <link rel="stylesheet" th:href="@{/user_template/css/style.css}"/>
</head>
<body>
    <!-- Header -->
    <div th:replace="user/layout/header :: header"></div>
    
    <!-- Main Content -->
    <main>
        <div class="container">
            <!-- Page content -->
            <th:block th:replace="${content}"></th:block>
        </div>
    </main>
    
    <!-- Footer -->
    <div th:replace="user/layout/footer :: footer"></div>
    
    <script th:src="@{/user_template/js/main.js}"></script>
</body>
</html>
```

---

## Common Thymeleaf Patterns

### 1. Conditional Rendering

```html
<!-- If condition -->
<div th:if="${user.isActive}">
    Active user
</div>

<!-- If-else -->
<div th:if="${book.price > 0}">
    <span th:text="${book.price}"></span> VND
</div>
<div th:unless="${book.price > 0}">
    Free
</div>

<!-- Switch -->
<div th:switch="${book.accessType}">
    <p th:case="'FREE'">Miễn phí</p>
    <p th:case="'PURCHASE'">Mua một lần</p>
    <p th:case="'SUBSCRIPTION'">VIP</p>
    <p th:case="*">Khác</p>
</div>
```

### 2. Iteration

```html
<!-- Simple loop -->
<ul>
    <li th:each="book : ${books}" th:text="${book.title}"></li>
</ul>

<!-- With index -->
<tr th:each="book, iterStat : ${books}">
    <td th:text="${iterStat.count}"></td>
    <td th:text="${book.title}"></td>
</tr>

<!-- With status variables -->
<div th:each="item, stat : ${items}">
    <span th:text="${stat.index}"></span>    <!-- 0-based index -->
    <span th:text="${stat.count}"></span>    <!-- 1-based count -->
    <span th:text="${stat.first}"></span>    <!-- boolean -->
    <span th:text="${stat.last}"></span>     <!-- boolean -->
    <span th:text="${stat.odd}"></span>      <!-- boolean -->
    <span th:text="${stat.even}"></span>     <!-- boolean -->
</div>
```

### 3. Forms

```html
<!-- Form with object binding -->
<form th:action="@{/admin/books/create}" th:object="${book}" method="post" enctype="multipart/form-data">
    <!-- CSRF token (automatic with Thymeleaf) -->
    
    <div class="form-group">
        <label for="title">Title</label>
        <input type="text" class="form-control" 
               th:field="*{title}" 
               th:errorclass="is-invalid"/>
        <div class="invalid-feedback" th:if="${#fields.hasErrors('title')}" 
             th:errors="*{title}"></div>
    </div>
    
    <div class="form-group">
        <label for="price">Price</label>
        <input type="number" class="form-control" th:field="*{price}"/>
    </div>
    
    <div class="form-group">
        <label for="category">Category</label>
        <select class="form-control" th:field="*{categoryId}">
            <option value="">-- Select --</option>
            <option th:each="cat : ${categories}" 
                    th:value="${cat.categoryId}" 
                    th:text="${cat.categoryName}"></option>
        </select>
    </div>
    
    <div class="form-group">
        <label>Cover Image</label>
        <input type="file" name="coverImage" accept="image/*"/>
    </div>
    
    <button type="submit" class="btn btn-primary">Save</button>
</form>
```

### 4. Security Integration

```html
<!-- Check if authenticated -->
<div sec:authorize="isAuthenticated()">
    <p>Welcome, <span sec:authentication="name"></span>!</p>
</div>

<!-- Check role -->
<div sec:authorize="hasRole('ADMIN')">
    <a th:href="@{/admin}">Admin Panel</a>
</div>

<!-- Check multiple roles -->
<div sec:authorize="hasAnyRole('USER', 'ADMIN')">
    <a th:href="@{/user/library}">My Library</a>
</div>

<!-- Not authenticated -->
<div sec:authorize="!isAuthenticated()">
    <a th:href="@{/auth/login}">Login</a>
</div>
```

### 5. Fragments

**Define Fragment**:
```html
<!-- file: fragments/book-card.html -->
<div th:fragment="bookCard(book)" class="book-card">
    <img th:src="${book.coverImageUrl}" alt="Cover"/>
    <h3 th:text="${book.title}"></h3>
    <p th:text="${book.price} + ' VND'"></p>
    <a th:href="@{/books/{id}(id=${book.id})}" class="btn btn-primary">View</a>
</div>
```

**Use Fragment**:
```html
<div th:each="book : ${books}">
    <div th:replace="fragments/book-card :: bookCard(${book})"></div>
</div>
```

### 6. Date Formatting

```html
<!-- Format date -->
<span th:text="${#temporals.format(book.createdAt, 'dd/MM/yyyy')}"></span>

<!-- Format datetime -->
<span th:text="${#temporals.format(book.createdAt, 'dd/MM/yyyy HH:mm')}"></span>
```

### 7. Number Formatting

```html
<!-- Format price -->
<span th:text="${#numbers.formatDecimal(book.price, 0, 'COMMA', 0, 'POINT')}"></span>

<!-- Format currency -->
<span th:text="${#numbers.formatCurrency(book.price)}"></span>
```

---

## Static Resources

### CSS Files

**Admin CSS**:
- `/admin_template/css/adminlte.min.css` - AdminLTE framework
- `/admin_template/css/custom.css` - Custom admin styles
- `/admin_template/plugins/fontawesome/css/all.min.css` - Icons

**User CSS**:
- `/user_template/css/bootstrap.min.css` - Bootstrap
- `/user_template/css/style.css` - Main stylesheet
- `/user_template/css/responsive.css` - Mobile responsive

**Shared CSS**:
- `/shared/css/common.css` - Common styles

### JavaScript Files

**Admin JS**:
- `/admin_template/js/jquery.min.js`
- `/admin_template/js/bootstrap.bundle.min.js`
- `/admin_template/js/adminlte.min.js`
- `/admin_template/js/custom.js`

**User JS**:
- `/user_template/js/jquery.min.js`
- `/user_template/js/bootstrap.bundle.min.js`
- `/user_template/js/main.js`

**Shared JS**:
- `/shared/js/common.js`

---

## Key Pages

### 1. Homepage (`home.html`)

**Route**: `/`, `/home`

**Content**:
- Hero banner
- Featured books
- New releases
- Best sellers
- Categories grid

**Example**:
```html
<div class="hero-section">
    <h1>Welcome to Ebook Store</h1>
    <p>Thousands of books at your fingertips</p>
</div>

<section class="featured-books">
    <h2>Featured Books</h2>
    <div class="row">
        <div class="col-md-3" th:each="book : ${featuredBooks}">
            <div class="book-card">
                <img th:src="${book.coverImageUrl}" class="img-fluid"/>
                <h4 th:text="${book.title}"></h4>
                <p th:text="${book.price} + ' VND'"></p>
                <a th:href="@{/books/{id}(id=${book.bookId})}" class="btn btn-primary">View</a>
            </div>
        </div>
    </div>
</section>
```

---

### 2. Login Page (`auth/login.html`)

**Route**: `/auth/login`

**Content**:
- Login form
- Error messages
- Register link

**Example**:
```html
<div class="login-container">
    <h2>Login</h2>
    
    <!-- Error message -->
    <div th:if="${param.error}" class="alert alert-danger">
        Invalid username or password
    </div>
    
    <!-- Success message -->
    <div th:if="${param.registered}" class="alert alert-success">
        Registration successful! Please login.
    </div>
    
    <form th:action="@{/auth/login}" method="post">
        <div class="form-group">
            <label>Username</label>
            <input type="text" name="username" class="form-control" required/>
        </div>
        
        <div class="form-group">
            <label>Password</label>
            <input type="password" name="password" class="form-control" required/>
        </div>
        
        <button type="submit" class="btn btn-primary btn-block">Login</button>
    </form>
    
    <p>Don't have an account? <a th:href="@{/auth/register}">Register</a></p>
</div>
```

---

### 3. Admin Dashboard (`admin/dashboard.html`)

**Route**: `/admin`, `/admin/dashboard`

**Content**:
- Statistics cards (users, books, orders, revenue)
- Recent orders table
- Charts (sales, popular books)

**Example**:
```html
<div class="content-header">
    <h1>Dashboard</h1>
</div>

<div class="content">
    <div class="row">
        <!-- Statistics Cards -->
        <div class="col-md-3">
            <div class="small-box bg-info">
                <div class="inner">
                    <h3 th:text="${stats.totalUsers}">0</h3>
                    <p>Total Users</p>
                </div>
                <div class="icon">
                    <i class="fas fa-users"></i>
                </div>
            </div>
        </div>
        
        <div class="col-md-3">
            <div class="small-box bg-success">
                <div class="inner">
                    <h3 th:text="${stats.totalBooks}">0</h3>
                    <p>Total Books</p>
                </div>
                <div class="icon">
                    <i class="fas fa-book"></i>
                </div>
            </div>
        </div>
        
        <!-- More cards... -->
    </div>
    
    <!-- Recent Orders -->
    <div class="card">
        <div class="card-header">
            <h3>Recent Orders</h3>
        </div>
        <div class="card-body">
            <table class="table">
                <thead>
                    <tr>
                        <th>Order ID</th>
                        <th>User</th>
                        <th>Amount</th>
                        <th>Status</th>
                        <th>Date</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="order : ${recentOrders}">
                        <td th:text="${order.orderId}"></td>
                        <td th:text="${order.user.fullName}"></td>
                        <td th:text="${order.finalAmount}"></td>
                        <td>
                            <span class="badge" 
                                  th:classappend="${order.status == 'COMPLETED' ? 'badge-success' : 'badge-warning'}"
                                  th:text="${order.status}"></span>
                        </td>
                        <td th:text="${#temporals.format(order.orderDate, 'dd/MM/yyyy')}"></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
```

---

### 4. Book List (Admin) (`admin/books/list.html`)

**Route**: `/admin/books`

**Content**:
- Search bar
- Filters (category, author, price range)
- Books table with actions
- Pagination

**Example**:
```html
<div class="card">
    <div class="card-header">
        <h3>Books Management</h3>
        <a th:href="@{/admin/books/create}" class="btn btn-success">
            <i class="fas fa-plus"></i> Add Book
        </a>
    </div>
    
    <div class="card-body">
        <!-- Search -->
        <form th:action="@{/admin/books}" method="get" class="mb-3">
            <div class="input-group">
                <input type="text" name="keyword" class="form-control" 
                       placeholder="Search books..." th:value="${param.keyword}"/>
                <button type="submit" class="btn btn-primary">Search</button>
            </div>
        </form>
        
        <!-- Books Table -->
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Cover</th>
                    <th>Title</th>
                    <th>Price</th>
                    <th>Category</th>
                    <th>Status</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="book : ${books.content}">
                    <td th:text="${book.bookId}"></td>
                    <td>
                        <img th:src="${book.coverImageUrl}" width="50"/>
                    </td>
                    <td th:text="${book.title}"></td>
                    <td th:text="${book.price}"></td>
                    <td th:text="${book.category.categoryName}"></td>
                    <td th:text="${book.accessType}"></td>
                    <td>
                        <a th:href="@{/admin/books/edit/{id}(id=${book.bookId})}" 
                           class="btn btn-sm btn-primary">
                            <i class="fas fa-edit"></i>
                        </a>
                        <button type="button" class="btn btn-sm btn-danger" 
                                th:onclick="'deleteBook(\'' + ${book.bookId} + '\')'">
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                </tr>
            </tbody>
        </table>
        
        <!-- Pagination -->
        <nav th:if="${books.totalPages > 1}">
            <ul class="pagination">
                <li class="page-item" th:classappend="${books.first} ? 'disabled'">
                    <a class="page-link" th:href="@{/admin/books(page=${books.number - 1})}">Previous</a>
                </li>
                <li class="page-item" th:each="i : ${#numbers.sequence(0, books.totalPages - 1)}"
                    th:classappend="${i == books.number} ? 'active'">
                    <a class="page-link" th:href="@{/admin/books(page=${i})}" th:text="${i + 1}"></a>
                </li>
                <li class="page-item" th:classappend="${books.last} ? 'disabled'">
                    <a class="page-link" th:href="@{/admin/books(page=${books.number + 1})}">Next</a>
                </li>
            </ul>
        </nav>
    </div>
</div>
```

---

## AJAX Integration

### Example: Delete Book with AJAX

**JavaScript**:
```javascript
function deleteBook(bookId) {
    if (!confirm('Are you sure you want to delete this book?')) {
        return;
    }
    
    $.ajax({
        url: '/admin/books/delete/' + bookId,
        type: 'POST',
        headers: {
            'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content')
        },
        success: function(response) {
            if (response.success) {
                alert('Book deleted successfully');
                location.reload();
            } else {
                alert('Error: ' + response.message);
            }
        },
        error: function() {
            alert('Error deleting book');
        }
    });
}
```

---

## Responsive Design

**Bootstrap Grid**:
```html
<div class="row">
    <div class="col-12 col-md-6 col-lg-3">
        <!-- 12 cols on mobile, 6 on tablet, 3 on desktop -->
    </div>
</div>
```

**Media Queries**:
```css
/* Mobile first */
.book-card {
    width: 100%;
}

@media (min-width: 768px) {
    .book-card {
        width: 50%;
    }
}

@media (min-width: 1200px) {
    .book-card {
        width: 25%;
    }
}
```

---

**Tài liệu liên quan**:
- [Cấu trúc dự án](PROJECT_STRUCTURE.md)
- [Kiến trúc hệ thống](ARCHITECTURE.md)
- [API Documentation](API_DOCUMENTATION.md)

