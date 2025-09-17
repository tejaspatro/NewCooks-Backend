# NewCooks Backend

This is the backend API for **NewCooks** — a recipe-sharing platform where **Chefs** can manage recipes and **Users** can browse, favorite, and explore recipes.

## 🚀 Features

- **User & Chef Authentication** (Sign Up, Login) using JWT
- Separate user flows:
  - **Chef**: Add, view, edit, and delete their own recipes; view statistics
  - **User**: Browse any recipes and add favorites
- Email verification via **Brevo (formerly Sendinblue)**
- Recipe management with categories, ingredients, and instructions
- Favorite recipes feature for users
- Clean separation of User and Chef roles

## ⚙️ Tech Stack

- **Spring Boot** (Java) for REST API
- **MySQL** (local development) and **PostgreSQL** (production)
- **JWT (JSON Web Tokens)** for authentication
- **Docker** for containerization
- **Maven** for build management
- **Postman** for API testing
- **Brevo (Sendinblue)** for email services

## 📦 Deployment

The backend is deployed on **Render.com** and connects to a PostgreSQL database hosted on Render as well.

## 📂 Project Structure

- `src/`: All source code
- `resources/`: Configurations (application.properties, etc.)
- `.env`: Environment variables (including DB connection, JWT secret, Brevo keys)
- `Dockerfile`: Docker image build instructions
- `pom.xml`: Maven build file

## 📧 Email Authentication

Brevo is used to send account activation emails after signup, ensuring verified users and chefs.

## 👨‍🍳👩‍🍳 User & Chef Flow

- **Chef**
    - Signup, Login, Email Activation
    - Add, Edit, Delete their own recipes
    - View recipe statistics (views, favorites, etc.)

- **User**
    - Signup, Login, Email Activation
    - Browse all recipes
    - Add recipes to favorites

---

This backend provides the core API logic to support the NewCooks frontend, focusing on scalability, security, and clean role separation.
