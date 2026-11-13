<template>
  <div class="not-found">
    <el-result icon="warning" title="页面不存在" sub-title="你访问的页面可能已被移除">
      <template #extra>
        <router-link to="/">
          <el-button type="primary">返回首页</el-button>
        </router-link>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
