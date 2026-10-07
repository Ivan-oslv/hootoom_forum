<template>
  <div class="app-shell">
    <AppHeader />
    <slot />
    <footer class="site-footer">
      © {{ new Date().getFullYear() }} HOOTOOM Forum
    </footer>
  </div>
</template>
