import result from '@/utils/result'
import type {
  ApiResult,
  ChangePasswordDTO,
  UpdateProfileDTO,
  UpdateSettingsDTO,
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

// TODO: 文件上传接口（Sprint 2 后期实现），底层由 FileService 按 storage.type 切换 MinIO/OSS
// export const uploadAvatar = (file: File) => {
//   const fd = new FormData()
//   fd.append('file', file)
//   return result.post<unknown, ApiResult<UploadAvatarVO>>('/upload/avatar', fd, {
//     headers: { 'Content-Type': 'multipart/form-data' },
//   })
// }
//
// export const uploadImage = (file: File) => {
//   const fd = new FormData()
//   fd.append('file', file)
//   return result.post<unknown, ApiResult<UploadImageVO>>('/upload/image', fd, {
//     headers: { 'Content-Type': 'multipart/form-data' },
//   })
// }
