/**
 * Device Fingerprinting Library
 * Tạo unique fingerprint cho thiết bị dựa trên các thuộc tính browser và hardware
 */

(function(window) {
    'use strict';

    const DeviceFingerprint = {
        /**
         * Tạo device fingerprint chính
         */
        async generate() {
            const components = await this.collectComponents();
            const fingerprintString = JSON.stringify(components);
            const hash = await this.sha256(fingerprintString);

            return {
                hash: hash,
                details: components
            };
        },

        /**
         * Thu thập các thành phần fingerprint
         */
        async collectComponents() {
            return {
                // Browser info
                userAgent: navigator.userAgent,
                language: navigator.language || navigator.userLanguage,
                languages: navigator.languages ? navigator.languages.join(',') : '',
                platform: navigator.platform,

                // Screen info
                screenResolution: `${screen.width}x${screen.height}`,
                colorDepth: screen.colorDepth,
                pixelRatio: window.devicePixelRatio || 1,

                // Timezone
                timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                timezoneOffset: new Date().getTimezoneOffset(),

                // Hardware
                hardwareConcurrency: navigator.hardwareConcurrency || 0,
                deviceMemory: navigator.deviceMemory || 0,

                // Browser features
                cookieEnabled: navigator.cookieEnabled,
                doNotTrack: navigator.doNotTrack || 'unknown',

                // Canvas fingerprint
                canvas: await this.getCanvasFingerprint(),

                // WebGL fingerprint
                webgl: this.getWebGLFingerprint(),

                // Fonts (simplified)
                fonts: this.detectFonts(),

                // Touch support
                touchSupport: this.getTouchSupport()
            };
        },

        /**
         * Canvas fingerprinting
         */
        async getCanvasFingerprint() {
            try {
                const canvas = document.createElement('canvas');
                canvas.width = 200;
                canvas.height = 50;
                const ctx = canvas.getContext('2d');

                ctx.textBaseline = 'top';
                ctx.font = '14px Arial';
                ctx.textBaseline = 'alphabetic';
                ctx.fillStyle = '#f60';
                ctx.fillRect(125, 1, 62, 20);
                ctx.fillStyle = '#069';
                ctx.fillText('Device Fingerprint 🔒', 2, 15);
                ctx.fillStyle = 'rgba(102, 204, 0, 0.7)';
                ctx.fillText('Device Fingerprint 🔒', 4, 17);

                const dataURL = canvas.toDataURL();
                return await this.simpleHash(dataURL);
            } catch (e) {
                return 'canvas-not-supported';
            }
        },

        /**
         * WebGL fingerprinting
         */
        getWebGLFingerprint() {
            try {
                const canvas = document.createElement('canvas');
                const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');

                if (!gl) {
                    return 'webgl-not-supported';
                }

                const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
                if (debugInfo) {
                    return {
                        vendor: gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL),
                        renderer: gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL)
                    };
                }

                return {
                    vendor: gl.getParameter(gl.VENDOR),
                    renderer: gl.getParameter(gl.RENDERER)
                };
            } catch (e) {
                return 'webgl-error';
            }
        },

        /**
         * Detect available fonts (simplified)
         */
        detectFonts() {
            const baseFonts = ['monospace', 'sans-serif', 'serif'];
            const testFonts = ['Arial', 'Courier New', 'Georgia', 'Times New Roman', 'Verdana'];
            const detected = [];

            const testString = "mmmmmmmmmmlli";
            const testSize = '72px';

            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');

            const baseWidths = {};
            for (let baseFont of baseFonts) {
                ctx.font = testSize + ' ' + baseFont;
                baseWidths[baseFont] = ctx.measureText(testString).width;
            }

            for (let testFont of testFonts) {
                let detected_font = false;
                for (let baseFont of baseFonts) {
                    ctx.font = testSize + ' ' + testFont + ', ' + baseFont;
                    const width = ctx.measureText(testString).width;
                    if (width !== baseWidths[baseFont]) {
                        detected_font = true;
                        break;
                    }
                }
                if (detected_font) {
                    detected.push(testFont);
                }
            }

            return detected.join(',');
        },

        /**
         * Touch support detection
         */
        getTouchSupport() {
            return {
                maxTouchPoints: navigator.maxTouchPoints || 0,
                touchEvent: 'ontouchstart' in window,
                touchStart: 'TouchEvent' in window
            };
        },

        /**
         * SHA-256 hash (using SubtleCrypto API)
         */
        async sha256(message) {
            const msgBuffer = new TextEncoder().encode(message);
            const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
            const hashArray = Array.from(new Uint8Array(hashBuffer));
            const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
            return hashHex;
        },

        /**
         * Simple hash function (fallback)
         */
        async simpleHash(str) {
            let hash = 0;
            for (let i = 0; i < str.length; i++) {
                const char = str.charCodeAt(i);
                hash = ((hash << 5) - hash) + char;
                hash = hash & hash;
            }
            return hash.toString(36);
        },

        /**
         * Detect device type
         */
        getDeviceType() {
            const ua = navigator.userAgent.toLowerCase();

            if (/(tablet|ipad|playbook|silk)|(android(?!.*mobi))/i.test(ua)) {
                return 'TABLET';
            }
            if (/Mobile|Android|iP(hone|od)|IEMobile|BlackBerry|Kindle|Silk-Accelerated|(hpw|web)OS|Opera M(obi|ini)/.test(ua)) {
                return 'MOBILE';
            }
            return 'WEB';
        },

        /**
         * Detect browser name
         */
        getBrowserName() {
            const ua = navigator.userAgent.toLowerCase();

            if (ua.indexOf('edg/') > -1 || ua.indexOf('edge') > -1) {
                return 'Edge';
            } else if (ua.indexOf('chrome') > -1 && ua.indexOf('edg') === -1) {
                return 'Chrome';
            } else if (ua.indexOf('firefox') > -1) {
                return 'Firefox';
            } else if (ua.indexOf('safari') > -1 && ua.indexOf('chrome') === -1) {
                return 'Safari';
            } else if (ua.indexOf('opera') > -1 || ua.indexOf('opr/') > -1) {
                return 'Opera';
            } else if (ua.indexOf('trident') > -1 || ua.indexOf('msie') > -1) {
                return 'Internet Explorer';
            } else {
                return 'Unknown';
            }
        },

        /**
         * Detect OS name
         */
        getOSName() {
            const ua = navigator.userAgent.toLowerCase();

            if (ua.indexOf('windows') > -1) {
                return 'Windows';
            } else if (ua.indexOf('mac os x') > -1 || ua.indexOf('macintosh') > -1) {
                return 'MacOS';
            } else if (ua.indexOf('linux') > -1 && ua.indexOf('android') === -1) {
                return 'Linux';
            } else if (ua.indexOf('android') > -1) {
                return 'Android';
            } else if (ua.indexOf('iphone') > -1 || ua.indexOf('ipad') > -1) {
                return 'iOS';
            } else {
                return 'Unknown';
            }
        },

        /**
         * Generate device name
         */
        getDeviceName() {
            const browser = this.getBrowserName();
            const os = this.getOSName();
            return `${browser} on ${os}`;
        }
    };

    // Export to window
    window.DeviceFingerprint = DeviceFingerprint;

})(window);

