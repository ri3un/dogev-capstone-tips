import { useState } from 'react'
import { dogApi } from '../api/endpoints'
import type { Dog } from '../api/types'
import { useApi } from './useApi'

const KEY = 'dogId'

/** 지금 보고 있는 반려견. 화면은 1마리 기준이고(결정 7), 여러 마리면 마지막에 고른 반려견을 기억합니다 */
export function useCurrentDog() {
  const { data: dogs, loading, error, reload } = useApi(() => dogApi.list(), [])
  const [selected, setSelected] = useState<number | null>(() => Number(localStorage.getItem(KEY)) || null)
  const dog: Dog | undefined = dogs?.find((d) => d.id === selected) ?? dogs?.[0]

  const select = (id: number) => {
    localStorage.setItem(KEY, String(id))
    setSelected(id)
  }
  return { dog, dogs: dogs ?? [], loading, error, reload, select }
}
