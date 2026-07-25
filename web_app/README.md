# ⚡ RapidTuy Web App & Portal (Versión Web)

Este directorio `/web_app/` contiene la versión Web/Landing interactiva completa de **RapidTuy** lista para ser desplegada en **Vercel**, **Netlify**, o cualquier servidor de hosting estático.

---

## 🚀 Pasos para Desplegar en Vercel (Gratis)

### Opción 1: Conectar Repositorio GitHub a Vercel (Recomendado)
1. Exporta o sube el contenido de esta carpeta `/web_app/` a tu repositorio en **GitHub**.
2. Ingresa a [vercel.com](https://vercel.com) e inicia sesión.
3. Haz clic en **"Add New"** -> **"Project"**.
4. Selecciona tu repositorio de GitHub.
5. Haz clic en **"Deploy"**. ¡Listo! Obtendrás tu enlace permanente tipo `https://rapidtuy.vercel.app`.

### Opción 2: Usar Vercel CLI (Línea de comandos)
Si tienes Node.js y la CLI de Vercel instalada en tu equipo local:
```bash
npm i -g vercel
cd web_app
vercel
```

---

## 📁 Archivos Incluidos en este Módulo Web

- `index.html`: Página principal responsiva con calculador de tarifas en tiempo real para Valles del Tuy.
- `style.css`: Sistema de diseño moderno en modo oscuro con acentos en Naranja RapidTuy (`#F97316`) y Verde Yummy (`#10B981`).
- `script.js`: Calculadora de tarifas interactivas, integración dinámica con la Central de WhatsApp y formateador de mensajes.
- `vercel.json`: Configuración lista para despliegue sin errores de rutas en Vercel.
