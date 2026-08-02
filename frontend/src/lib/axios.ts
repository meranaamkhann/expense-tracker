import axios, { AxiosRequestConfig, AxiosResponse } from "axios";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const api = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("accessToken");
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Client-side Mock Store for simulating backend endpoints not present in the Spring Boot backend
const getMockData = (key: string, defaultVal: any) => {
  if (typeof window === "undefined") return defaultVal;
  const val = localStorage.getItem(`mock_${key}`);
  return val ? JSON.parse(val) : defaultVal;
};

const setMockData = (key: string, data: any) => {
  if (typeof window !== "undefined") {
    localStorage.setItem(`mock_${key}`, JSON.stringify(data));
  }
};

// Initial default categories
const DEFAULT_CATEGORIES = [
  { id: 1, name: "Food & Drinks", color: "#f59e0b", icon: "Utensils" },
  { id: 2, name: "Shopping", color: "#ec4899", icon: "ShoppingBag" },
  { id: 3, name: "Housing", color: "#3b82f6", icon: "Home" },
  { id: 4, name: "Transportation", color: "#10b981", icon: "Car" },
  { id: 5, name: "Entertainment", color: "#8b5cf6", icon: "Tv" },
  { id: 6, name: "Salary", color: "#22c55e", icon: "DollarSign" },
];

// Initialize Mock Store
if (typeof window !== "undefined") {
  if (!localStorage.getItem("mock_users")) {
    setMockData("users", [
      {
        id: "1",
        email: "demo@spendwise.com",
        name: "Asad Khan",
        password: "password123",
      },
    ]);
  }
  if (!localStorage.getItem("mock_categories")) {
    setMockData("categories", DEFAULT_CATEGORIES);
  }
  if (!localStorage.getItem("mock_expenses")) {
    setMockData("expenses", [
      { id: 1, title: "Groceries", amount: 120.5, category: "Food & Drinks", date: "2026-07-15" },
      { id: 2, title: "Uniqlo Jacket", amount: 89.99, category: "Shopping", date: "2026-07-14" },
      { id: 3, title: "Monthly Rent", amount: 1200.0, category: "Housing", date: "2026-07-01" },
      { id: 4, title: "Uber Ride", amount: 24.5, category: "Transportation", date: "2026-07-16" },
      { id: 5, title: "Netflix Subscription", amount: 15.49, category: "Entertainment", date: "2026-07-12" },
      { id: 6, title: "Salary Payout", amount: 4500.0, category: "Salary", date: "2026-07-01" },
    ]);
  }
}

// Interceptor to handle Mock Requests and Server Fallback
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config as AxiosRequestConfig & { _retry?: boolean };

    // Check if error is due to Server Connection Failure (network error or status >= 500)
    const isNetworkError = !error.response;
    const isServerUnavailable = error.response && error.response.status >= 500;
    const url = config.url || "";

    // 1. If it's a backend request for Expenses and the server fails/is offline, fallback to localStorage
    if (url.includes("/api/expenses") && (isNetworkError || isServerUnavailable)) {
      console.warn("Spring Boot backend offline/unavailable. Falling back to local storage for expenses.");
      
      const localExpenses = getMockData("expenses", []);

      // Simulating GET /api/expenses
      if (config.method === "get" || config.method === "GET") {
        return Promise.resolve({
          data: localExpenses,
          status: 200,
          statusText: "OK",
          headers: {},
          config,
        } as AxiosResponse);
      }

      // Simulating POST /api/expenses
      if (config.method === "post" || config.method === "POST") {
        const body = JSON.parse(config.data || "{}");
        const newExpense = {
          ...body,
          id: localExpenses.length > 0 ? Math.max(...localExpenses.map((e: any) => e.id)) + 1 : 1,
          date: body.date || new Date().toISOString().split("T")[0],
        };
        const updatedExpenses = [...localExpenses, newExpense];
        setMockData("expenses", updatedExpenses);
        return Promise.resolve({
          data: newExpense,
          status: 201,
          statusText: "Created",
          headers: {},
          config,
        } as AxiosResponse);
      }

      // Simulating PUT /api/expenses/{id}
      if (config.method === "put" || config.method === "PUT") {
        const id = parseInt(url.split("/").pop() || "0");
        const body = JSON.parse(config.data || "{}");
        const updatedExpenses = localExpenses.map((e: any) =>
          e.id === id ? { ...e, ...body } : e
        );
        setMockData("expenses", updatedExpenses);
        const updatedExpense = updatedExpenses.find((e: any) => e.id === id) || body;
        return Promise.resolve({
          data: updatedExpense,
          status: 200,
          statusText: "OK",
          headers: {},
          config,
        } as AxiosResponse);
      }

      // Simulating DELETE /api/expenses/{id}
      if (config.method === "delete" || config.method === "DELETE") {
        const id = parseInt(url.split("/").pop() || "0");
        const updatedExpenses = localExpenses.filter((e: any) => e.id !== id);
        setMockData("expenses", updatedExpenses);
        return Promise.resolve({
          data: { message: "Expense deleted successfully" },
          status: 200,
          statusText: "OK",
          headers: {},
          config,
        } as AxiosResponse);
      }
    }

    // 2. Intercept Auth & Settings endpoints since Spring Boot doesn't implement them
    if (url.includes("/api/auth/") || url.includes("/api/users/") || url.includes("/api/categories")) {
      const users = getMockData("users", []);
      
      // Simulate Register
      if (url.includes("/api/auth/register")) {
        const body = JSON.parse(config.data || "{}");
        if (users.find((u: any) => u.email === body.email)) {
          return Promise.reject({
            response: {
              status: 400,
              data: { message: "Email already registered" },
            },
          });
        }
        const newUser = {
          id: Math.random().toString(36).substr(2, 9),
          email: body.email,
          name: body.name,
          password: body.password,
        };
        users.push(newUser);
        setMockData("users", users);
        return Promise.resolve({
          data: { message: "User registered successfully" },
          status: 201,
          config,
        } as AxiosResponse);
      }

      // Simulate Login
      if (url.includes("/api/auth/login")) {
        const body = JSON.parse(config.data || "{}");
        const user = users.find((u: any) => u.email === body.email && u.password === body.password);
        if (!user) {
          return Promise.reject({
            response: {
              status: 401,
              data: { message: "Invalid email or password" },
            },
          });
        }
        return Promise.resolve({
          data: {
            accessToken: "mock-jwt-access-token",
            refreshToken: "mock-jwt-refresh-token",
            user: { id: user.id, email: user.email, name: user.name },
          },
          status: 200,
          config,
        } as AxiosResponse);
      }

      // Simulate Token Refresh
      if (url.includes("/api/auth/refresh")) {
        return Promise.resolve({
          data: {
            accessToken: "mock-jwt-access-token-new",
            refreshToken: "mock-jwt-refresh-token-new",
          },
          status: 200,
          config,
        } as AxiosResponse);
      }

      // Simulate User Profile
      if (url.includes("/api/users/profile")) {
        const token = localStorage.getItem("accessToken");
        if (!token) {
          return Promise.reject({ response: { status: 401, data: { message: "Unauthorized" } } });
        }
        // Just return the first user or mock demo user
        const demoUser = users[0];
        
        if (config.method === "get" || config.method === "GET") {
          return Promise.resolve({
            data: { id: demoUser.id, email: demoUser.email, name: demoUser.name },
            status: 200,
            config,
          } as AxiosResponse);
        }

        if (config.method === "put" || config.method === "PUT") {
          const body = JSON.parse(config.data || "{}");
          
          if (url.includes("/password")) {
            // Change password simulation
            const { currentPassword, newPassword } = body;
            const targetUser = users.find((u: any) => u.password === currentPassword);
            if (!targetUser) {
              return Promise.reject({
                response: { status: 400, data: { message: "Current password is incorrect" } }
              });
            }
            targetUser.password = newPassword;
            setMockData("users", users);
            return Promise.resolve({
              data: { message: "Password updated successfully" },
              status: 200,
              config,
            } as AxiosResponse);
          } else {
            // General profile update
            const updatedUsers = users.map((u: any) => 
              u.id === demoUser.id ? { ...u, name: body.name, email: body.email } : u
            );
            setMockData("users", updatedUsers);
            return Promise.resolve({
              data: { id: demoUser.id, email: body.email, name: body.name },
              status: 200,
              config,
            } as AxiosResponse);
          }
        }
      }

      // Simulate Category Management
      if (url.includes("/api/categories")) {
        const categories = getMockData("categories", DEFAULT_CATEGORIES);

        if (config.method === "get" || config.method === "GET") {
          return Promise.resolve({
            data: categories,
            status: 200,
            config,
          } as AxiosResponse);
        }

        if (config.method === "post" || config.method === "POST") {
          const body = JSON.parse(config.data || "{}");
          const newCategory = {
            ...body,
            id: categories.length > 0 ? Math.max(...categories.map((c: any) => c.id)) + 1 : 1,
          };
          const updatedCategories = [...categories, newCategory];
          setMockData("categories", updatedCategories);
          return Promise.resolve({
            data: newCategory,
            status: 201,
            config,
          } as AxiosResponse);
        }

        if (config.method === "put" || config.method === "PUT") {
          const id = parseInt(url.split("/").pop() || "0");
          const body = JSON.parse(config.data || "{}");
          const updatedCategories = categories.map((c: any) =>
            c.id === id ? { ...c, ...body } : c
          );
          setMockData("categories", updatedCategories);
          const updatedCategory = updatedCategories.find((c: any) => c.id === id) || body;
          return Promise.resolve({
            data: updatedCategory,
            status: 200,
            config,
          } as AxiosResponse);
        }

        if (config.method === "delete" || config.method === "DELETE") {
          const id = parseInt(url.split("/").pop() || "0");
          const updatedCategories = categories.filter((c: any) => c.id !== id);
          setMockData("categories", updatedCategories);
          return Promise.resolve({
            data: { message: "Category deleted successfully" },
            status: 200,
            config,
          } as AxiosResponse);
        }
      }
    }

    return Promise.reject(error);
  }
);
