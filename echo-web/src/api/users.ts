import result from '@/utils/result'
import type {
  ApiResult,
  ChangePasswordDTO,
  FollowVO,
  UpdateProfileDTO,
  UpdateSettingsDTO,
  UploadAvatarVO,
  UploadImageVO,
  UserProfileVO,
  UserSettingsVO,
} from './modules'

export const getProfile = () => result.get<unknown, ApiResult<UserProfileVO>>('/users/me/profile')

export const updateProfile = (dto: UpdateProfileDTO) =>
  result.put<unknown, ApiResult<void>>('/users/me/profile', dto)

export const getSettings = () =>
  result.get<unknown, ApiResult<UserSettingsVO>>('/users/me/settings')

export const updateSettings = (dto: UpdateSettingsDTO) =>
  result.put<unknown, ApiResult<void>>('/users/me/settings', dto)

export const changePassword = (dto: ChangePasswordDTO) =>
  result.put<unknown, ApiResult<void>>('/users/me/password', dto)

export const uploadAvatar = (file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return result.post<unknown, ApiResult<UploadAvatarVO>>('/upload/avatar', fd)
}

export const uploadImage = (file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return result.post<unknown, ApiResult<UploadImageVO>>('/upload/image', fd)
}

export const deleteFile = (url: string) =>
  result.delete<unknown, ApiResult<void>>('/upload/file', { params: { url } })

export const followUser = (id: number) =>
  result.post<unknown, ApiResult<FollowVO>>(`/users/${id}/follow`)
