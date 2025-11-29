/**
 * Auth Pages Common JavaScript
 * Handles form validation, interactions, and UI enhancements
 */

// Form Validation
class AuthFormValidator {
  constructor(formId) {
    this.form = document.getElementById(formId);
    if (this.form) {
      this.form.addEventListener('submit', (e) => this.validateForm(e));
    }
  }

  validateForm(e) {
    // This will be overridden by child classes
  }

  showError(message) {
    console.error(message);
    alert(message);
  }
}

// Login Form Validation
class LoginValidator extends AuthFormValidator {
  constructor() {
    super('loginForm');
  }

  validateForm(e) {
    const username = this.form.querySelector('input[name="username"]').value.trim();
    const password = this.form.querySelector('input[name="password"]').value;

    if (!username) {
      e.preventDefault();
      this.showError('Vui lòng nhập tên đăng nhập!');
      return false;
    }

    if (username.length < 3) {
      e.preventDefault();
      this.showError('Tên đăng nhập phải có ít nhất 3 ký tự!');
      return false;
    }

    if (!password) {
      e.preventDefault();
      this.showError('Vui lòng nhập mật khẩu!');
      return false;
    }

    if (password.length < 6) {
      e.preventDefault();
      this.showError('Mật khẩu phải có ít nhất 6 ký tự!');
      return false;
    }

    return true;
  }
}

// Register Form Validation
class RegisterValidator extends AuthFormValidator {
  constructor() {
    super('registerForm');
  }

  validateForm(e) {
    const username = document.querySelector('input[name="username"]')?.value.trim() || '';
    const email = document.querySelector('input[name="email"]')?.value.trim() || '';
    const password = document.querySelector('input[name="password"]')?.value || '';
    const termsCheckbox = document.getElementById('terms');

    // Validate username
    if (!username) {
      e.preventDefault();
      this.showError('Vui lòng nhập tên đăng nhập!');
      return false;
    }

    if (username.length < 3) {
      e.preventDefault();
      this.showError('Tên đăng nhập phải có ít nhất 3 ký tự!');
      return false;
    }

    // Validate email
    if (!email) {
      e.preventDefault();
      this.showError('Vui lòng nhập email!');
      return false;
    }

    if (!this.isValidEmail(email)) {
      e.preventDefault();
      this.showError('Email không hợp lệ!');
      return false;
    }

    // Validate password
    if (!password) {
      e.preventDefault();
      this.showError('Vui lòng nhập mật khẩu!');
      return false;
    }

    if (password.length < 6) {
      e.preventDefault();
      this.showError('Mật khẩu phải có ít nhất 6 ký tự!');
      return false;
    }

    // Validate terms
    if (termsCheckbox && !termsCheckbox.checked) {
      e.preventDefault();
      this.showError('Vui lòng đồng ý với Điều khoản sử dụng và Chính sách bảo mật!');
      return false;
    }

    return true;
  }

  isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }
}

// Alert Dismissal
function initializeAlertDismissal() {
  const alertCloseButtons = document.querySelectorAll('.btn-close');
  alertCloseButtons.forEach((button) => {
    button.addEventListener('click', function () {
      const alert = this.closest('[class*="alert"]');
      if (alert) {
        alert.style.transition = 'opacity 0.3s ease, max-height 0.3s ease';
        alert.style.opacity = '0';
        alert.style.maxHeight = '0';
        alert.style.overflow = 'hidden';
        setTimeout(() => {
          alert.remove();
        }, 300);
      }
    });
  });
}

// Password visibility toggle
// Password visibility toggle
function initializePasswordToggle() {
  const passwordInputs = document.querySelectorAll('input[type="password"]');
  passwordInputs.forEach((input) => {
    const container = input.closest('.form-group');

    // Kiểm tra xem đã có nút toggle chưa để tránh tạo trùng lặp
    if (container && !container.querySelector('.password-toggle')) {

      const toggleButton = document.createElement('button');
      toggleButton.type = 'button';
      toggleButton.className = 'password-toggle'; // Sử dụng class CSS đã tạo ở trên
      toggleButton.innerHTML = '<i class="fa fa-eye"></i>';

      toggleButton.addEventListener('click', (e) => {
        e.preventDefault();
        // Toggle type giữa password và text
        const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
        input.setAttribute('type', type);

        // Toggle icon
        toggleButton.innerHTML = type === 'password'
            ? '<i class="fa fa-eye"></i>'
            : '<i class="fa fa-eye-slash"></i>';
      });

      // Thêm padding cho input để chữ không đè lên icon
      input.style.paddingRight = '40px';

      // Chèn nút vào container
      container.appendChild(toggleButton);
    }
  });
}

// Remember Me functionality
function initializeRememberMe() {
  const rememberMeCheckbox = document.querySelector('input[name="remember-me"]');
  if (rememberMeCheckbox) {
    // Load saved username
    const savedUsername = localStorage.getItem('savedUsername');
    const usernameInput = document.querySelector('input[name="username"]');

    if (savedUsername && usernameInput) {
      usernameInput.value = savedUsername;
      rememberMeCheckbox.checked = true;
    }

    // Save username on login
    const loginForm = document.querySelector('form[action*="/login"]');
    if (loginForm) {
      loginForm.addEventListener('submit', () => {
        if (rememberMeCheckbox.checked && usernameInput) {
          localStorage.setItem('savedUsername', usernameInput.value);
        } else {
          localStorage.removeItem('savedUsername');
        }
      });
    }
  }
}

// Input focus effect
function initializeFocusEffects() {
  const inputs = document.querySelectorAll('.form-control');
  inputs.forEach((input) => {
    input.addEventListener('focus', function () {
      this.style.boxShadow = '0 0 0 3px rgba(255, 107, 53, 0.1)';
    });

    input.addEventListener('blur', function () {
      this.style.boxShadow = 'none';
    });
  });
}

// Form submission feedback
function initializeFormFeedback() {
  const forms = document.querySelectorAll('form');
  forms.forEach((form) => {
    form.addEventListener('submit', function () {
      const submitButton = this.querySelector('button[type="submit"]');
      if (submitButton) {
        const originalText = submitButton.innerHTML;
        submitButton.disabled = true;
        submitButton.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang xử lý...';

        // Reset after 3 seconds in case of error
        setTimeout(() => {
          submitButton.disabled = false;
          submitButton.innerHTML = originalText;
        }, 3000);
      }
    });
  });
}

// Keyboard shortcuts
function initializeKeyboardShortcuts() {
  document.addEventListener('keydown', (e) => {
    // Ctrl/Cmd + Enter to submit form
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      const form = document.querySelector('form');
      if (form) {
        form.submit();
      }
    }

    // Escape to dismiss alerts
    if (e.key === 'Escape') {
      const alerts = document.querySelectorAll('[class*="alert"]');
      alerts.forEach((alert) => {
        alert.remove();
      });
    }
  });
}

// Smooth transitions for modals/overlays
function initializeTransitions() {
  const style = document.createElement('style');
  style.textContent = `
    * {
      transition: background-color 0.3s ease, color 0.3s ease, border-color 0.3s ease;
    }
    
    .btn:active {
      transform: scale(0.98);
    }
  `;
  document.head.appendChild(style);
}

// Initialize all features on page load
document.addEventListener('DOMContentLoaded', () => {
  // Auto-initialize validators if forms exist
  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');

  if (loginForm) {
    new LoginValidator();
  }

  // Comment out register validation temporarily for testing
  // if (registerForm) {
  //   new RegisterValidator();
  // }

  // Initialize all interactive features
  initializeAlertDismissal();
  initializePasswordToggle();
  initializeRememberMe();
  initializeFocusEffects();
  initializeFormFeedback();
  initializeKeyboardShortcuts();
  initializeTransitions();

  console.log('Auth pages initialized successfully');
});

