import result from '@/utils/result'
import type {
  ApiResult,
  LoginDTO,
  LoginVO,
  MeVO,
  RefreshDTO,
  RefreshVO,
  RegisterDTO,
} from './modules'

export const login = (loginDTO: LoginDTO) =>
  result.post<unknown, ApiResult<LoginVO>>('/auth/login', loginDTO)

export const register = (registerDTO: RegisterDTO) =>
  result.post<unknown, ApiResult<void>>('/auth/register', registerDTO)

export const refresh = (refreshDTO: RefreshDTO) =>
  result.post<unknown, ApiResult<RefreshVO>>('/auth/refresh', refreshDTO)

export const me = () => result.get<unknown, ApiResult<MeVO>>('/auth/me')
