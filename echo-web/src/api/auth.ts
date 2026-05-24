import result from '@/utils/result'
import type { ApiResult, LoginDTO, LoginVO, meVO, RefreshVO, RegisterDTO } from './modules'

export const login = (loginDTO: LoginDTO) =>
  result.post<unknown, ApiResult<LoginVO>>('/auth/login', loginDTO)

export const register = (registerDTO: RegisterDTO) =>
  result.post<unknown, ApiResult<void>>('/auth/register', registerDTO)

export const refresh = (refresh: string) =>
  result.post<unknown, ApiResult<RefreshVO>>('/auth/refresh', refresh)

export const me = () => result.get<unknown, ApiResult<meVO>>('/auth/me')
