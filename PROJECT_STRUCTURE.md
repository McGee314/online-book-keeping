# Project Folder Structure & Best Practices

---

## Backend: Spring Boot Project Layout

```
bookkeeping-backend/
├── src/
│   ├── main/
│   │   ├── java/com/yourname/bookkeeping/
│   │   │   ├── BookkeepingApplication.java    ← main class + CORS bean
│   │   │   │
│   │   │   ├── common/                        ← shared utilities
│   │   │   │   ├── Result.java                ← unified API response wrapper
│   │   │   │   ├── ResultCode.java            ← error code constants
│   │   │   │   └── GlobalExceptionHandler.java← @RestControllerAdvice
│   │   │   │
│   │   │   ├── config/                        ← Spring config beans
│   │   │   │   ├── MybatisPlusConfig.java     ← pagination plugin
│   │   │   │   └── WebMvcConfig.java          ← JWT filter registration
│   │   │   │
│   │   │   ├── security/                      ← auth concerns only
│   │   │   │   ├── JwtUtil.java               ← generate/parse JWT
│   │   │   │   └── JwtFilter.java             ← OncePerRequestFilter
│   │   │   │
│   │   │   ├── entity/                        ← @TableName JPA-like POJOs
│   │   │   │   ├── User.java
│   │   │   │   ├── Category.java
│   │   │   │   ├── Transaction.java
│   │   │   │   └── Budget.java
│   │   │   │
│   │   │   ├── dto/                           ← request/response shapes
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── TransactionRequest.java
│   │   │   │   └── StatsSummaryVO.java        ← VO = view object (response)
│   │   │   │
│   │   │   ├── mapper/                        ← MyBatis-Plus mappers
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── CategoryMapper.java
│   │   │   │   ├── TransactionMapper.java
│   │   │   │   └── BudgetMapper.java
│   │   │   │
│   │   │   ├── service/                       ← business logic interfaces
│   │   │   │   ├── UserService.java
│   │   │   │   ├── CategoryService.java
│   │   │   │   ├── TransactionService.java
│   │   │   │   ├── BudgetService.java
│   │   │   │   └── impl/                      ← implementations
│   │   │   │       ├── UserServiceImpl.java
│   │   │   │       ├── CategoryServiceImpl.java
│   │   │   │       ├── TransactionServiceImpl.java
│   │   │   │       └── BudgetServiceImpl.java
│   │   │   │
│   │   │   └── controller/                    ← REST endpoints
│   │   │       ├── AuthController.java        ← /api/auth/*
│   │   │       ├── CategoryController.java    ← /api/categories/*
│   │   │       ├── TransactionController.java ← /api/transactions/*
│   │   │       ├── BudgetController.java      ← /api/budgets/*
│   │   │       └── StatsController.java       ← /api/stats/*
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml            ← dev overrides
│   │       └── mapper/                        ← XML files for custom SQL
│   │           └── TransactionMapper.xml
│   │
│   └── test/java/com/yourname/bookkeeping/
│       └── service/
│           └── TransactionServiceTest.java
│
└── pom.xml
```

### Key Backend Rules

1. **Never put business logic in controllers.** Controllers should only: validate input, call a service, and return `Result<T>`. Logic (queries, calculations, permission checks) lives in the Service layer.

2. **Use DTOs/VOs to separate API contracts from DB entities.** Never return a raw `User` entity — it would expose the hashed password. Create a `UserVO` with only safe fields.

3. **Store the current user in a thread-local context.** In `JwtFilter`, after parsing the token, store the userId in a `UserContext` class backed by `ThreadLocal<Long>`. Services then call `UserContext.getUserId()` instead of accepting userId as a parameter. This prevents users from accessing each other's data.

```java
// In JwtFilter after token validation:
UserContext.setUserId(userId);

// In any service:
Long userId = UserContext.getUserId();
LambdaQueryWrapper<Transaction> wrapper = new LambdaQueryWrapper<Transaction>()
    .eq(Transaction::getUserId, userId)
    .eq(Transaction::getDeleted, 0);
```

---

## Frontend: Vue 3 Project Layout

```
bookkeeping-frontend/
├── public/
│   └── favicon.ico
│
├── src/
│   ├── main.js                    ← app entry, register plugins
│   ├── App.vue                    ← root component
│   │
│   ├── router/
│   │   └── index.js               ← routes + navigation guard
│   │
│   ├── stores/                    ← Pinia stores
│   │   ├── user.js                ← auth state (token, userInfo)
│   │   └── app.js                 ← global UI state (sidebar collapsed, etc.)
│   │
│   ├── api/                       ← one file per backend resource
│   │   ├── auth.js                ← login(), register()
│   │   ├── transaction.js         ← getList(), create(), update(), remove()
│   │   ├── category.js
│   │   ├── budget.js
│   │   └── stats.js
│   │
│   ├── utils/
│   │   ├── request.js             ← Axios instance (see scaffolding guide)
│   │   └── format.js              ← currency, date formatters
│   │
│   ├── layouts/
│   │   └── AppLayout.vue          ← sidebar + header shell
│   │
│   ├── views/                     ← page-level components (routed)
│   │   ├── auth/
│   │   │   ├── LoginPage.vue
│   │   │   └── RegisterPage.vue
│   │   ├── dashboard/
│   │   │   └── DashboardPage.vue
│   │   ├── transaction/
│   │   │   └── TransactionListPage.vue
│   │   ├── category/
│   │   │   └── CategoryPage.vue
│   │   ├── budget/
│   │   │   └── BudgetPage.vue
│   │   └── profile/
│   │       └── UserProfilePage.vue
│   │
│   └── components/                ← reusable, dumb components
│       ├── charts/
│       │   ├── BarLineChart.vue   ← wraps ECharts bar/line
│       │   └── PieChart.vue       ← wraps ECharts pie
│       ├── TransactionFormDialog.vue
│       └── SummaryCard.vue
│
├── vite.config.js
├── package.json
└── .env.development               ← VITE_API_BASE_URL=http://localhost:8080
```

### Key Frontend Rules

1. **Put all API calls in `src/api/`, never directly in components.** Components import from `src/api/transaction.js` and call `transactionApi.getList(params)`. This means if the backend URL changes, you fix it in one file, not across 10 components.

```javascript
// src/api/transaction.js
import request from '@/utils/request'

export const transactionApi = {
  getList: (params) => request.get('/transactions', { params }),
  create:  (data)   => request.post('/transactions', data),
  update:  (id, data) => request.put(`/transactions/${id}`, data),
  remove:  (id)     => request.delete(`/transactions/${id}`),
}
```

2. **Separate views (pages) from components.** `views/` contains routed page components — they are allowed to be "fat" and wire together multiple API calls and child components. `components/` contains pure presentational components that receive data via props and emit events. A `PieChart.vue` should only care about rendering the chart from its `option` prop — it should not know about Axios or Pinia.

3. **Use `<script setup>` consistently.** It gives you the most concise syntax with Vue 3's Composition API. Pair it with `defineProps` / `defineEmits` for component contracts. Avoid the Options API — mixing styles in one project creates confusion.

```vue
<!-- Good: clean script setup pattern -->
<script setup>
import { ref, onMounted } from 'vue'
import { transactionApi } from '@/api/transaction'

const props = defineProps({ userId: Number })
const emit = defineEmits(['refresh'])

const list = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  const res = await transactionApi.getList({ userId: props.userId })
  list.value = res.data
  loading.value = false
})
</script>
```

---

## Git Repository Structure

```
bookkeeping/                   ← monorepo root
├── bookkeeping-backend/       ← Spring Boot project
├── bookkeeping-frontend/      ← Vue 3 project
├── db/
│   ├── schema.sql
│   └── seed.sql
├── SPRINT_PLAN.md
├── SCAFFOLDING_GUIDE.md
├── PROJECT_STRUCTURE.md
├── README.md
└── .gitignore
```

### `.gitignore` (root level)

```gitignore
# Maven
bookkeeping-backend/target/
bookkeeping-backend/.mvn/

# Node
bookkeeping-frontend/node_modules/
bookkeeping-frontend/dist/

# IDE
.idea/
*.iml
.vscode/settings.json

# Environment / secrets
*.env
application-prod.yml

# OS
.DS_Store
Thumbs.db
```
