import { ref, type Ref } from 'vue'
import type { PostVO } from '@/api/modules/index'
import { fetchPosts as apiFetchPosts } from '@/api/posts'

export type { PostVO, PostAuthor } from '@/api/modules/index'

export interface UseInfiniteListOptions {
  /** 自定义 fetch 函数，返回包含 data 字段的响应体（对齐 result.ts 拦截器返回值） */
  fetchFn?: (params: Record<string, unknown>) => Promise<{ data: Record<string, unknown> }>
  /** 叠加到每次请求的固定参数 */
  baseParams?: Record<string, unknown>
  /** 分页模式：'cursor'=游标（默认），'page'=传统页码 */
  mode?: 'cursor' | 'page'
  /** 每页条数，默认 10 */
  pageSize?: number
}

const DEFAULT_PAGE_SIZE = 10

export function useInfiniteList<T = PostVO>(options?: UseInfiniteListOptions) {
  const fetchFn: (params: Record<string, unknown>) => Promise<{ data: Record<string, unknown> }> =
    (options?.fetchFn ?? apiFetchPosts) as (
      params: Record<string, unknown>,
    ) => Promise<{ data: Record<string, unknown> }>
  const baseParams = ref<Record<string, unknown>>(options?.baseParams ?? {})
  const mode = options?.mode ?? 'cursor'
  const PAGE_SIZE = options?.pageSize ?? DEFAULT_PAGE_SIZE

  const posts: Ref<T[]> = ref([])
  const loading = ref(false)
  const hasMore = ref(true)
  const cursor = ref<string | null>(null)
  const currentPage = ref(1)
  const error = ref<string | null>(null)

  // ---- post-specific local toggle state (unused for non-post types) ----
  const likedPosts = ref<Set<number>>(new Set())
  const collectedPosts = ref<Set<number>>(new Set())

  function setBaseParam(key: string, value: unknown): void {
    baseParams.value = { ...baseParams.value, [key]: value }
    reset()
  }

  async function fetchPosts(): Promise<void> {
    if (loading.value || (!hasMore.value && posts.value.length > 0)) return

    loading.value = true
    error.value = null

    try {
      const params: Record<string, unknown> = { ...baseParams.value, size: PAGE_SIZE }
      if (mode === 'page') {
        params.current = currentPage.value
      } else {
        params.cursor = cursor.value
      }

      const res = await fetchFn(params)
      const result = res.data as Record<string, unknown>

      const records = result.records as T[]
      if (records && records.length > 0) {
        posts.value.push(...records)
      }

      if (mode === 'page') {
        const total = result.total as number
        const cur = result.current as number
        const sz = (result.size as number) || PAGE_SIZE
        hasMore.value = cur * sz < total
        currentPage.value++
      } else {
        cursor.value = (result.cursor as string) ?? null
        hasMore.value = (result.hasMore as boolean) ?? false
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载失败'
    } finally {
      loading.value = false
    }
  }

  function toggleLike(postId: number): void {
    const next = new Set(likedPosts.value)
    if (next.has(postId)) {
      next.delete(postId)
    } else {
      next.add(postId)
    }
    likedPosts.value = next
  }

  function toggleCollect(postId: number): void {
    const next = new Set(collectedPosts.value)
    if (next.has(postId)) {
      next.delete(postId)
    } else {
      next.add(postId)
    }
    collectedPosts.value = next
  }

  function reset(): void {
    posts.value = []
    cursor.value = null
    currentPage.value = 1
    hasMore.value = true
    loading.value = false
    error.value = null
    likedPosts.value = new Set()
    collectedPosts.value = new Set()
  }

  return {
    posts,
    loading,
    hasMore,
    cursor,
    error,
    likedPosts,
    collectedPosts,
    fetchPosts,
    toggleLike,
    toggleCollect,
    reset,
    setBaseParam,
  }
}
