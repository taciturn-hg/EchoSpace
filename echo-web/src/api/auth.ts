import result from '@/utils/result'
import type { ApiResult, LoginDTO, LoginVO, RegisterDTO } from './modules'

export const login = (loginDTO: LoginDTO) =>
  result.post<unknown, ApiResult<LoginVO>>('/auth/login', loginDTO)

export const register = (registerDTO: RegisterDTO) =>
  result.post<unknown, ApiResult<void>>('/auth/register', registerDTO)
