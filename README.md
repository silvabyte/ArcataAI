# **Arcata Web Project - Developer Guide**

Welcome to the Arcata Web Project! This guide is designed to help you set up the project locally and understand our development practices.

## **Prerequisites**

Ensure you have Node.js installed (preferably the latest stable version).

## **Setting Up the Project**

1.  **Install Dependencies**
    Our project is managed by NX, [click here to learn more](/README_NX.md)

        In the project directory, run:

        ```bash
        npm install -g nx@latest
        npm install
        ```

        This installs all necessary dependencies.

2.  **Environment Setup**

    Create a **`.env`** file in the root of the project with the following content:

    ```
    VITE_SUPABASE_KEY=ask_mat_for_the_key
    ```

    _Note: Obtain the **`VITE_SUPABASE_KEY`** value from Mat._

3.  **Running the Development Server for a given app**

    Start the development server for auth server:

    ```bash
    nx server auth
    ```

    Start the development server for hq:

    ```bash
    nx server hq
    ```

    Access the application in your browser at **`http://localhost:4201/`**.

4.  **Building for Production**

    Build an for production:

    ```bash
    nx build auth
    ```

5.  **Linting**
    Ensure code adheres to standards:

    ```bash
    nx run auth:lint
    ```

6.  **Preview Production Build**

    Preview the production build:

    ```bash
    nx run auth:build
    nx run auth:preview
    ```

## **Key Dependencies**

-   **React & React Router**: For building user interfaces and managing navigation.
-   **Vite**: Modern frontend build tool.
-   **Tailwind CSS**: Utility-first CSS framework.
-   **Supabase**: Open-source alternative for database and authentication.
-   **i18next**: Internationalization-framework.
-   **Joi**: Object schema description and validation.

## **Contributing**

Mostly open up a pr at this pint....

---

Happy coding! 🚀

## **Project Structure Overview**

Here's a breakdown of the key directories and files in the project:

```bash
arcata-web/
├── README.md                 # Project overview and setup guide
├── index.html                # Entry point for the HTML structure
├── package-lock.json         # Locked versions of npm dependencies
├── package.json              # Project dependencies and scripts
├── postcss.config.js         # Configuration for PostCSS
├── public/                   # Public assets like favicon and locales
│   ├── favicon.ico
│   ├── locales/
│   │   └── en.json           # English language resources
│   └── vite.svg
├── src/                      # Source code of the application
│   ├── assets/               # Static assets like images and CSS
│   │   ├── react.svg
│   │   └── tailwind.css
│   ├── components/           # Reusable UI components
...
│   │   ├── css.ts            # Css helper fns
│   │   ├── errors/
│   │   │   └── AppErrorOutlet.tsx
│   │   └── support/
│   │       └── SupportLink.tsx
│   ├── config.ts             # Application configuration settings
│   ├── db/                   # Database related functionalities
│   │   ├── auth.ts
│   │   ├── client.ts
│   │   ├── database.types.ts
│   │   ├── generateResourceApi.ts
│   │   ├── index.ts
│   │   └── resourceTypes.ts
│   ├── env.ts                # Environment variables handling
│   ├── index.css             # Global CSS styles
│   ├── main.tsx              # Main entry point for React components
│   ├── meta.tsx              # Meta tags and other HTML head elements
│   ├── routes/               # Application routes and pages
│   │   ├── App.tsx
│   │   ├── hq/
│   │   │   ├── HQ.tsx
│   │   │   └── kanban/
│   │   │       ├── KanBanLane.css
│   │   │       └── KanbanLane.tsx
│   │   ├── jobs/
│   │   │   ├── AddJobByUrl.tsx
│   │   │   └── index.ts
│   │   └── session/
│   │       ├── SessionAuthenticate.tsx
│   │       ├── SessionLogin.tsx
│   │       └── SessionVerify.tsx
│   ├── translate/            # Internationalization setup
│   │   ├── i18n.ts
│   │   └── index.ts
│   └── vite-env.d.ts
├── tailwind.config.js        # Tailwind CSS configuration
├── tsconfig.json             # TypeScript configuration
├── tsconfig.node.json        # TypeScript configuration for Node.js
└── vite.config.ts            # Vite build tool configuration

```

## **Key Points to Note**

-   **Components**: The

**`src/components/`** directory contains reusable UI components, organized into subdirectories like **`alerts`**, **`brand`**, **`buttons`**, etc. This modular structure helps in maintaining a clean and scalable codebase.

-   **Routing and Pages**: The **`src/routes/`** directory holds the different pages of the application, structured according to their respective routes. This includes main application routes (**`App.tsx`**), specific feature areas like **`hq`** and **`jobs`**, and session management routes under **`session`**.
-   **Database and Authentication**: The **`src/db/`** directory contains files related to database operations and authentication logic. This centralization of database-related code aids in easier management and updates.
-   **Localization**: The **`public/locales/`** directory, specifically **`en.json`**, holds the language resources, supporting internationalization efforts.
-   **Styling**: Tailwind CSS is used for styling, with its configuration in **`tailwind.config.js`** and global styles in **`src/assets/tailwind.css`**. Component-specific styles are located within their respective component directories.
-   **Environment Variables**: **`src/env.ts`** is used for handling environment variables, ensuring sensitive information like API keys are securely managed.
-   **Static Assets**: The **`src/assets/`** directory contains static assets like images and global CSS files.
-   **TypeScript Configuration**: TypeScript is used for type-checking, with its configuration specified in **`tsconfig.json`** and **`tsconfig.node.json`**.
-   **Vite Configuration**: The project uses Vite as the build tool, configured in **`vite.config.ts`**.
