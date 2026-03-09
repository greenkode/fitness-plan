export function useCalendar() {
  const weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

  const formatDay = (isoDate: string) => {
    const parsed = new Date(isoDate)
    return `${weekdays[parsed.getDay()]} • ${parsed.getDate()}`
  }

  return { formatDay }
}
