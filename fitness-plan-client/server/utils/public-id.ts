import { nanoid } from 'nanoid'

export function generatePublicId(): string {
  return nanoid(21)
}
