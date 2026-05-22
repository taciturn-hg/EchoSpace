import { AxiosHeaders, type AxiosRequestConfig } from 'axios'

export function setHeader(config: AxiosRequestConfig, key: string, value: string) {
  if (!config.headers) {
    config.headers = {}
  }

  if (config.headers instanceof AxiosHeaders) {
    config.headers.set(key, value)
  } else {
    config.headers[key] = value
  }
}
