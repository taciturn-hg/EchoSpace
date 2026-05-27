<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import { Bold, Italic, Heading2, Quote, Code2, ImagePlus, Link2 } from '@lucide/vue'
import { createPost } from '@/api/posts'
import { uploadImage } from '@/api/users'

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png']
const MAX_IMAGE_SIZE = 10 * 1024 * 1024 // 10MB
const ALLOWED_LINK_PROTOCOLS = ['http:', 'https:', 'mailto:', 'tel:']

const router = useRouter()

const title = ref('')
const hasContent = ref(false)
const publishing = ref(false)
const imageInput = ref<HTMLInputElement | null>(null)

// ===== TipTap Editor =====
const editor = useEditor({
  content: '',
  extensions: [
    StarterKit,
    Image.configure({
      allowBase64: false,
      inline: false,
    }),
    Link.configure({
      openOnClick: false,
      HTMLAttributes: { rel: 'noopener noreferrer' },
      validate: (href: string) => ALLOWED_LINK_PROTOCOLS.some((p) => href.toLowerCase().startsWith(p)),
    }),
  ],
  editorProps: {
    handlePaste: (_view, event) => {
      const items = event.clipboardData?.items
      if (!items) return false
      let hasImage = false
      for (const item of items) {
        if (item.type.startsWith('image/')) {
          hasImage = true
          const file = item.getAsFile()
          if (file) uploadAndInsert(file)
        }
      }
      if (hasImage) event.preventDefault()
      return hasImage
    },
    handleDrop: (_view, event) => {
      const files = event.dataTransfer?.files
      if (!files?.length) return false
      let hasImage = false
      for (const file of files) {
        if (file.type.startsWith('image/')) {
          hasImage = true
          uploadAndInsert(file)
        }
      }
      if (hasImage) event.preventDefault()
      return hasImage
    },
  },
  onUpdate: ({ editor: ed }) => {
    const html = ed.getHTML()
    hasContent.value = html !== '<p></p>' && html.trim().length > 0
  },
})

// ===== Toolbar Handlers =====
function toggleBold() {
  editor.value?.chain().focus().toggleBold().run()
}

function toggleItalic() {
  editor.value?.chain().focus().toggleItalic().run()
}

function toggleHeading() {
  editor.value?.chain().focus().toggleHeading({ level: 2 }).run()
}

function toggleBlockquote() {
  editor.value?.chain().focus().toggleBlockquote().run()
}

function toggleCodeBlock() {
  editor.value?.chain().focus().toggleCodeBlock().run()
}

function triggerImageUpload() {
  imageInput.value?.click()
}

function handleImageFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files?.length) return
  for (const file of files) {
    if (file.type.startsWith('image/')) {
      uploadAndInsert(file)
    }
  }
  input.value = ''
}

function setLink() {
  const previousUrl = editor.value?.getAttributes('link').href as string | undefined
  const url = window.prompt('输入链接地址', previousUrl || 'https://')
  if (url === null) return
  if (url === '') {
    editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }
  if (!ALLOWED_LINK_PROTOCOLS.some((p) => url.toLowerCase().startsWith(p))) {
    ElMessage.warning('仅支持 http、https、mailto、tel 协议的链接')
    return
  }
  editor.value?.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

// ===== Image Upload =====
async function uploadAndInsert(file: File) {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG、GIF、WebP 格式')
    return
  }
  if (file.size > MAX_IMAGE_SIZE) {
    ElMessage.error('图片大小不能超过 10MB')
    return
  }
  try {
    const res = await uploadImage(file)
    const url = res.data!.url
    editor.value?.chain().focus().setImage({ src: url }).run()
  } catch {
    ElMessage.error('图片上传失败，请稍后重试')
  }
}

// ===== Computed =====
const canPublish = computed(() => {
  return editor.value && title.value.trim().length > 0 && hasContent.value && !publishing.value
})

// ===== Actions =====
function handleBack() {
  router.push('/')
}

function handleSave() {
  ElMessage.info('功能开发中')
}

async function handlePublish() {
  if (!canPublish.value) return

  publishing.value = true
  try {
    const html = editor.value!.getHTML()
    const res = await createPost({ title: title.value.trim(), contentHtml: html })
    ElMessage.success('发布成功')
    router.push(`/post/${res.data!.id}`)
  } catch {
    ElMessage.error('发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}
</script>

<template>
  <div class="post-create">
    <!-- Header Row -->
    <div class="post-create__header">
      <button class="post-create__back" @click="handleBack" aria-label="返回首页">&lt;</button>
      <div class="post-create__actions">
        <button class="post-create__save" disabled @click="handleSave">保存</button>
        <button
          :class="['post-create__publish', { 'post-create__publish--active': canPublish }]"
          :disabled="!canPublish"
          @click="handlePublish"
        >
          {{ publishing ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>

    <!-- Main Card -->
    <div class="post-create__card">
      <!-- Title Field -->
      <div class="post-create__field">
        <label class="post-create__label">标题</label>
        <input
          v-model="title"
          class="post-create__title-input"
          type="text"
          placeholder="输入标题（1 ~ 200 字）"
          maxlength="200"
        />
      </div>

      <!-- Content Field -->
      <div class="post-create__field post-create__field--body">
        <label class="post-create__label">正文</label>

        <!-- Toolbar -->
        <div v-if="editor" class="post-create__toolbar">
          <button
            :class="['toolbar-btn', { 'is-active': editor.isActive('bold') }]"
            title="粗体"
            @click="toggleBold"
          >
            <Bold :size="16" />
          </button>
          <button
            :class="['toolbar-btn', { 'is-active': editor.isActive('italic') }]"
            title="斜体"
            @click="toggleItalic"
          >
            <Italic :size="16" />
          </button>
          <span class="toolbar-divider" />
          <button
            :class="['toolbar-btn', { 'is-active': editor.isActive('heading', { level: 2 }) }]"
            title="标题"
            @click="toggleHeading"
          >
            <Heading2 :size="16" />
          </button>
          <button
            :class="['toolbar-btn', { 'is-active': editor.isActive('blockquote') }]"
            title="引用"
            @click="toggleBlockquote"
          >
            <Quote :size="16" />
          </button>
          <button
            :class="['toolbar-btn', { 'is-active': editor.isActive('codeBlock') }]"
            title="代码块"
            @click="toggleCodeBlock"
          >
            <Code2 :size="16" />
          </button>
          <span class="toolbar-divider" />
          <button class="toolbar-btn" title="图片" @click="triggerImageUpload">
            <ImagePlus :size="16" />
          </button>
          <input
            ref="imageInput"
            type="file"
            :accept="ALLOWED_IMAGE_TYPES.join(',')"
            class="toolbar-file-input"
            @change="handleImageFileChange"
          />
          <button
            :class="['toolbar-btn', { 'is-active': editor.isActive('link') }]"
            title="链接"
            @click="setLink"
          >
            <Link2 :size="16" />
          </button>
        </div>

        <!-- Editor -->
        <div class="post-create__editor">
          <EditorContent :editor="editor" class="post-create__editor-content" />
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
// ===== Design Tokens =====
$bg-card: #ffffff;
$bg-page: #f8f9fa;
$border-color: #e1e4e8;
$text-primary: #1a1a2e;
$text-secondary: #6b7280;
$text-muted: #9ca3af;
$accent: #2563eb;
$radius-card: 12px;
$radius-sm: 8px;
$transition-fast: 150ms ease;

// ===== Page Container =====
.post-create {
  max-width: 880px;
  margin: 0 auto;
  height: calc(100vh - 64px - 48px);
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.35s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// ===== Header Row =====
.post-create__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  padding-bottom: 16px;
}

.post-create__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid $border-color;
  border-radius: $radius-sm;
  background: $bg-card;
  color: $text-secondary;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition:
    background $transition-fast,
    border-color $transition-fast,
    color $transition-fast;

  &:hover {
    background: #f3f4f6;
    border-color: #d1d5db;
    color: $text-primary;
  }
}

// ===== Action Buttons =====
.post-create__actions {
  display: flex;
  gap: 12px;
}

.post-create__save {
  padding: 8px 24px;
  border: 1px solid $border-color;
  border-radius: $radius-sm;
  background: #f3f4f6;
  color: $text-muted;
  font-size: 14px;
  font-weight: 500;
  cursor: not-allowed;
  transition:
    background $transition-fast,
    color $transition-fast;
}

.post-create__publish {
  padding: 8px 24px;
  border: none;
  border-radius: $radius-sm;
  background: #e5e7eb;
  color: $text-muted;
  font-size: 14px;
  font-weight: 500;
  cursor: not-allowed;
  transition:
    background $transition-fast,
    color $transition-fast,
    box-shadow $transition-fast;

  &--active {
    background: $accent;
    color: #ffffff;
    cursor: pointer;

    &:hover {
      background: #1d4ed8;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
    }

    &:active {
      transform: scale(0.98);
    }
  }
}

// ===== Main Card =====
.post-create__card {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-card;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

// ===== Field Box =====
.post-create__field {
  background: #fafbfc;
  border: 1px solid #eceff4;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;

  &--body {
    flex: 1;
    min-height: 0;
    padding-bottom: 6px;
  }
}

.post-create__label {
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
  margin-bottom: 10px;
  letter-spacing: 0.02em;
}

// ===== Toolbar =====
.post-create__toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-bottom: 10px;
  margin-bottom: 4px;
  border-bottom: 1px solid #eceff4;
  flex-shrink: 0;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: $text-secondary;
  cursor: pointer;
  transition:
    background $transition-fast,
    color $transition-fast;

  &:hover {
    background: #e8ecf1;
    color: $text-primary;
  }

  &.is-active {
    background: #dbeafe;
    color: $accent;
  }
}

.toolbar-divider {
  display: inline-block;
  width: 1px;
  height: 18px;
  background: #e1e4e8;
  margin: 0 4px;
}

.toolbar-file-input {
  display: none;
}

// ===== Title Input =====
.post-create__title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 18px;
  font-weight: 500;
  color: $text-primary;
  line-height: 1.5;
  padding: 0;
  background: transparent;

  &::placeholder {
    color: $text-muted;
    font-weight: 400;
  }
}

// ===== Editor =====
.post-create__editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  margin: 0 -20px;
}

.post-create__editor-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.ProseMirror) {
    flex: 1;
    overflow-y: auto;
    padding: 12px 20px;
    outline: none;
    font-size: 15px;
    color: $text-primary;
    line-height: 1.85;
    word-break: break-word;

    p {
      margin: 0 0 12px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    h1 {
      font-size: 24px;
      font-weight: 700;
      margin: 24px 0 12px;
      line-height: 1.4;
    }

    h2 {
      font-size: 20px;
      font-weight: 600;
      margin: 20px 0 10px;
      line-height: 1.4;
    }

    h3 {
      font-size: 17px;
      font-weight: 600;
      margin: 16px 0 8px;
      line-height: 1.4;
    }

    h4 {
      font-size: 15px;
      font-weight: 600;
      margin: 14px 0 6px;
      line-height: 1.4;
    }

    blockquote {
      margin: 12px 0;
      padding: 8px 16px;
      border-left: 3px solid #d1d5db;
      color: $text-secondary;
      background: #f9fafb;
      border-radius: 0 6px 6px 0;
    }

    pre {
      margin: 12px 0;
      padding: 16px;
      background: #1e1e2e;
      color: #cdd6f4;
      border-radius: 6px;
      overflow-x: auto;
      font-size: 13px;
      line-height: 1.6;
    }

    code {
      font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
      font-size: 0.9em;

      &:not(pre code) {
        padding: 2px 6px;
        background: #f3f4f6;
        border-radius: 4px;
        color: #e91e63;
      }
    }

    pre code {
      padding: 0;
      background: transparent;
      color: inherit;
    }

    ul,
    ol {
      margin: 10px 0;
      padding-left: 24px;
    }

    li {
      margin: 4px 0;
    }

    a {
      color: $accent;
      text-decoration: underline;
      cursor: pointer;
    }

    hr {
      border: none;
      border-top: 1px solid #f0f0f0;
      margin: 20px 0;
    }

    img {
      max-width: 100%;
      height: auto;
      border-radius: 6px;
      margin: 8px 0;

      &.ProseMirror-selectednode {
        outline: 2px solid $accent;
        outline-offset: 2px;
      }
    }
  }
}
</style>
