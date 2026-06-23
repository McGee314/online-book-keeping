# Project Scaffolding Guide

## Prerequisites

Verify these are installed before starting:

```bash
java -version        # Need JDK 11 or 17
mvn -version         # Maven 3.6+
node -version        # Node 18+
npm -version
mysql --version      # MySQL 5.7 or 8.0
```

---

## Part 1 — Backend: Spring Boot + MyBatis-Plus

### Step 1: Generate the project skeleton

Go to [https://start.spring.io](https://start.spring.io) and configure:

| Field | Value |
|-------|-------|
| Project | Maven |
| Language | Java |
| Spring Boot | 2.7.x (latest 2.x) |
| Group | com.yourname |
| Artifact | bookkeeping |
| Packaging | Jar |
| Java | 11 or 17 |

Add these dependencies on the site:
- Spring Web
- MySQL Driver
- Lombok
- Validation

Then click **Generate**, unzip, and open in IntelliJ IDEA.

Alternatively, use the Spring CLI if you have it:

```bash
spring init \
  --dependencies=web,mysql,lombok,validation \
  --group-id=com.yourname \
  --artifact-id=bookkeeping \
  --java-version=11 \
  --build=maven \
  --boot-version=2.7.18 \
  bookkeeping-backend
```

### Step 2: Add MyBatis-Plus and JWT dependencies

Open `pom.xml` and add inside `<dependencies>`:

```xml
<!-- MyBatis-Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- BCrypt password hashing (Spring Security crypto only, no full security) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>

<!-- Hutool utilities (optional but useful) -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.25</version>
</dependency>
```

### Step 3: Configure `application.yml`

Rename `src/main/resources/application.properties` to `application.yml`, then paste:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookkeeping?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: your_password_here   # change this
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl   # remove in production
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted    # MyBatis-Plus soft delete
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: auto

jwt:
  secret: "YourSuperSecretKeyThatIsAtLeast256BitsLong!BookkeepingApp2024"
  expiration: 86400000   # 24 hours in milliseconds

logging:
  level:
    com.yourname.bookkeeping: debug
```

### Step 4: Verify the backend starts

```bash
cd bookkeeping-backend
mvn spring-boot:run
```

Expected: `Started BookkeepingApplication in X.XXX seconds`

---

## Part 2 — Frontend: Vue 3 + Vite

### Step 1: Scaffold with Vite

```bash
npm create vite@latest bookkeeping-frontend -- --template vue
cd bookkeeping-frontend
npm install
```

### Step 2: Install all required dependencies

```bash
# UI component library
npm install element-plus@2.6.3

# Auto-import plugins (saves writing imports everywhere)
npm install -D unplugin-vue-components@0.26.0 unplugin-auto-import@0.17.6

# Routing and state
npm install vue-router@4.3.0 pinia@2.1.7

# HTTP client
npm install axios@1.6.8

# Charts
npm install echarts@5.4.3

# Date utility
npm install dayjs@1.11.10
```

### Step 3: Configure auto-imports for Element Plus in `vite.config.js`

Replace the contents of `vite.config.js`:

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Proxy all /api calls to the Spring Boot backend during development
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### Step 4: Create the Axios instance

Create `src/utils/request.js`:

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// Attach JWT token to every request
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// Global error handling
request.interceptors.response.use(
  response => response.data,
  error => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.error('Session expired, please log in again.')
    } else {
      ElMessage.error(error.response?.data?.message || 'Request failed')
    }
    return Promise.reject(error)
  }
)

export default request
```

### Step 5: Verify the frontend starts

```bash
npm run dev
```

Expected: `VITE v5.x.x  ready in XXX ms  ➜  Local: http://localhost:5173/`

---

## Spring Boot CORS Configuration

Add this bean to your main application class or a `@Configuration` class:

```java
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOriginPattern("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return new CorsFilter(source);
}
```

---

## MyBatis-Plus Quick-Start Pattern

For every entity, you only need 3 files (no boilerplate SQL for basic CRUD):

```java
// 1. Entity
@Data
@TableName("transactions")
public class Transaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long categoryId;
    private Integer type;           // 1=income, 2=expense
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String note;
    @TableLogic                     // enables soft delete automatically
    private Integer deleted;
}

// 2. Mapper — zero methods needed for basic CRUD
@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> { }

// 3. Service
@Service
public class TransactionServiceImpl
        extends ServiceImpl<TransactionMapper, Transaction>
        implements TransactionService {
    // list, getById, save, updateById, removeById all inherited
}
```

---

## Unified API Response Wrapper

Create `com.yourname.bookkeeping.common.Result`:

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
```

Every controller method returns `Result<T>`, so the frontend always gets a consistent JSON shape:
```json
{ "code": 200, "message": "success", "data": { ... } }
```
