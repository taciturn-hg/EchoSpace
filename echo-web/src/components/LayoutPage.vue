<script setup lang="ts">
// 后续在此添加 NavBar、Sidebar 等布局组件
</script>

<template>
  <div class="layout">
    <main class="layout-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.layout-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}
</style>
