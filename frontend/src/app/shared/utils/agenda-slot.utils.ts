import { AgendaDia } from '../../core/models';

export type AgendaSlotStatus = 'LIVRE' | 'RESERVADO' | 'BLOQUEADO' | 'ENCERRADO';

export interface AgendaSlot {
  index: number;
  inicio: string;
  fim: string;
  status: AgendaSlotStatus;
  detalhe: string;
}

interface BuildAgendaSlotsOptions {
  openingTime: string | null | undefined;
  closingTime: string | null | undefined;
  fallbackOpeningMinutes: number;
  fallbackClosingMinutes: number;
  slotDurationMinutes: number;
  isPastSlot: (slotStartMinutes: number, slotEndMinutes: number) => boolean;
}

export function toggleContiguousSlotSelection(selectedIndexes: number[], slot: AgendaSlot): number[] {
  if (slot.status !== 'LIVRE') {
    return selectedIndexes;
  }

  const selected = [...selectedIndexes].sort((left, right) => left - right);
  const alreadySelected = selected.includes(slot.index);

  if (alreadySelected) {
    if (selected.length === 1) {
      return [];
    }

    if (slot.index === selected[0]) {
      return selected.slice(1);
    }

    if (slot.index === selected[selected.length - 1]) {
      return selected.slice(0, -1);
    }

    return [slot.index];
  }

  if (selected.length === 0) {
    return [slot.index];
  }

  const first = selected[0];
  const last = selected[selected.length - 1];

  if (slot.index === first - 1) {
    return [slot.index, ...selected];
  }

  if (slot.index === last + 1) {
    return [...selected, slot.index];
  }

  return [slot.index];
}

export function slotStatusLabel(status: AgendaSlotStatus, selected: boolean): string {
  if (selected) {
    return 'Selecionado';
  }

  switch (status) {
    case 'LIVRE':
      return 'Livre';
    case 'BLOQUEADO':
      return 'Bloqueado';
    case 'ENCERRADO':
      return 'Concluído';
    default:
      return 'Reservado';
  }
}

export function buildAgendaSlots(
  agenda: AgendaDia,
  {
    openingTime,
    closingTime,
    fallbackOpeningMinutes,
    fallbackClosingMinutes,
    slotDurationMinutes,
    isPastSlot
  }: BuildAgendaSlotsOptions
): AgendaSlot[] {
  const slots: AgendaSlot[] = [];
  const startMinutes = resolveTimeToMinutes(openingTime, fallbackOpeningMinutes);
  const endMinutes = resolveTimeToMinutes(closingTime, fallbackClosingMinutes);

  let index = 0;
  for (let start = startMinutes; start + slotDurationMinutes <= endMinutes; start += slotDurationMinutes) {
    const end = start + slotDurationMinutes;
    const inicio = minutesToTime(start);
    const fim = minutesToTime(end);
    const bloqueio = agenda.bloqueios.find((item) => hasOverlap(start, end, item.horarioInicio, item.horarioFim));
    const reserva = agenda.reservasAtivas.find((item) => hasOverlap(start, end, item.horarioInicio, item.horarioFim));

    if (bloqueio) {
      slots.push({
        index,
        inicio,
        fim,
        status: 'BLOQUEADO',
        detalhe: bloqueio.motivo?.trim() || ''
      });
      index += 1;
      continue;
    }

    if (reserva) {
      slots.push({
        index,
        inicio,
        fim,
        status: 'RESERVADO',
        detalhe: ''
      });
      index += 1;
      continue;
    }

    if (isPastSlot(start, end)) {
      slots.push({
        index,
        inicio,
        fim,
        status: 'ENCERRADO',
        detalhe: 'Horário concluído.'
      });
      index += 1;
      continue;
    }

    slots.push({
      index,
      inicio,
      fim,
      status: 'LIVRE',
      detalhe: ''
    });
    index += 1;
  }

  return slots;
}

export function buildSelectedIntervalLabel(selectedSlots: AgendaSlot[], emptyLabel = 'Nenhum horário selecionado'): string {
  if (selectedSlots.length === 0) {
    return emptyLabel;
  }

  const first = selectedSlots[0];
  const last = selectedSlots[selectedSlots.length - 1];
  return `${first.inicio} às ${last.fim}`;
}

export function formatTimeRange(start: string | null | undefined, end: string | null | undefined): string {
  return `${normalizeTime(start)} às ${normalizeTime(end)}`;
}

export function normalizeTime(value: string | null | undefined): string {
  return (value ?? '').slice(0, 5);
}

export function timeToMinutes(value: string): number {
  const [hours, minutes] = value.split(':').map((part) => Number(part));
  return hours * 60 + minutes;
}

export function minutesToTime(value: number): string {
  const hours = Math.floor(value / 60)
    .toString()
    .padStart(2, '0');
  const minutes = (value % 60).toString().padStart(2, '0');
  return `${hours}:${minutes}`;
}

function resolveTimeToMinutes(value: string | null | undefined, fallback: number): number {
  const normalized = normalizeTime(value);
  if (!normalized) {
    return fallback;
  }

  return timeToMinutes(normalized);
}

function hasOverlap(slotStart: number, slotEnd: number, start: string, end: string): boolean {
  const faixaInicio = timeToMinutes(start);
  const faixaFim = timeToMinutes(end);
  return slotStart < faixaFim && slotEnd > faixaInicio;
}
