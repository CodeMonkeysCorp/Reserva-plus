export function toDateInputValue(date: Date): string {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return localDate.toISOString().split('T')[0];
}

export function currentDateInputValue(): string {
  return toDateInputValue(new Date());
}

export function addDaysToDateInput(dateValue: string, days: number): string {
  const date = new Date(`${dateValue}T00:00:00`);
  date.setDate(date.getDate() + days);
  return toDateInputValue(date);
}

export function addDaysToCurrentDate(days: number): string {
  return addDaysToDateInput(currentDateInputValue(), days);
}

export function addWeeksToDateInput(dateValue: string, weeks: number): string {
  return addDaysToDateInput(dateValue, weeks * 7);
}
