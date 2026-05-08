import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, Input, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

interface CalendarDay {
  date: string;
  dayNumber: number;
  inCurrentMonth: boolean;
  disabled: boolean;
  today: boolean;
}

@Component({
  selector: 'app-date-field',
  standalone: true,
  imports: [CommonModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DateFieldComponent),
      multi: true
    }
  ],
  template: `
    <div class="date-shell" [class.is-open]="isOpen" [class.is-disabled]="disabled">
      <button
        type="button"
        class="date-trigger"
        [class.is-placeholder]="!value"
        [disabled]="disabled"
        [attr.aria-expanded]="isOpen"
        aria-haspopup="dialog"
        (click)="toggle()"
        (blur)="markAsTouched()"
        (keydown)="onTriggerKeydown($event)"
      >
        <span>{{ displayValue || placeholder }}</span>
        <span class="date-icon" aria-hidden="true"></span>
      </button>

      <div class="date-panel" *ngIf="isOpen" role="dialog" aria-label="Calendário">
        <div class="date-head">
          <button type="button" class="nav-button" (click)="previousMonth()" [disabled]="!canGoPreviousMonth()">‹</button>
          <strong>{{ monthLabel }}</strong>
          <button type="button" class="nav-button" (click)="nextMonth()" [disabled]="!canGoNextMonth()">›</button>
        </div>

        <div class="weekdays">
          <span *ngFor="let weekday of weekdays">{{ weekday }}</span>
        </div>

        <div class="calendar-grid">
          <button
            type="button"
            class="day-cell"
            *ngFor="let day of calendarDays"
            [class.is-outside]="!day.inCurrentMonth"
            [class.is-selected]="day.date === value"
            [class.is-today]="day.today && day.date !== value"
            [disabled]="day.disabled"
            (click)="selectDay(day.date)"
            (keydown)="onDayKeydown($event, day.date)"
          >
            {{ day.dayNumber }}
          </button>
        </div>

        <div class="date-actions">
          <button type="button" class="link-button" *ngIf="clearable && value" (click)="clear()">Limpar</button>
          <span *ngIf="!(clearable && value)"></span>
          <button type="button" class="link-button" (click)="goToToday()">Hoje</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .date-shell {
      position: relative;
    }

    .date-trigger {
      width: 100%;
      min-height: 3.35rem;
      border: 1px solid var(--field-border);
      border-radius: 16px;
      padding: 0.85rem 1rem;
      background: var(--field-surface);
      color: var(--text);
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.85),
        0 1px 0 rgba(255, 255, 255, 0.5);
      transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
      font: inherit;
      text-align: left;
    }

    .date-trigger:hover:not(:disabled),
    .date-shell.is-open .date-trigger {
      border-color: var(--field-border-strong);
      box-shadow: var(--field-shadow);
    }

    .date-shell.is-open .date-trigger {
      transform: translateY(-1px);
    }

    .date-trigger:disabled {
      border-color: rgba(29, 43, 36, 0.08);
      background: linear-gradient(180deg, #f4f7f5 0%, #edf3ef 100%);
      color: #6f857a;
      cursor: not-allowed;
      box-shadow: none;
    }

    .date-trigger.is-placeholder {
      color: #82958a;
    }

    .date-icon {
      position: relative;
      width: 1.15rem;
      height: 1.05rem;
      border: 2px solid #264b3d;
      border-radius: 0.3rem;
      flex: 0 0 auto;
    }

    .date-icon::before,
    .date-icon::after {
      content: "";
      position: absolute;
      top: -0.35rem;
      width: 2px;
      height: 0.45rem;
      background: #264b3d;
      border-radius: 999px;
    }

    .date-icon::before {
      left: 0.2rem;
    }

    .date-icon::after {
      right: 0.2rem;
    }

    .date-panel {
      position: absolute;
      z-index: 30;
      top: calc(100% + 0.45rem);
      right: 0;
      width: min(21rem, 100vw - 2rem);
      padding: 0.75rem;
      border-radius: 22px;
      border: 1px solid rgba(29, 43, 36, 0.12);
      background:
        linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 250, 247, 0.98)),
        #ffffff;
      box-shadow: 0 24px 48px rgba(8, 26, 20, 0.18);
      backdrop-filter: blur(12px);
    }

    .date-head,
    .date-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
    }

    .date-head {
      margin-bottom: 0.75rem;
    }

    .date-head strong {
      text-transform: capitalize;
      font-size: 1rem;
    }

    .nav-button,
    .link-button,
    .day-cell {
      font: inherit;
    }

    .nav-button {
      width: 2.2rem;
      height: 2.2rem;
      border: 0;
      border-radius: 999px;
      background: rgba(15, 107, 77, 0.08);
      color: var(--text);
      font-size: 1.2rem;
      transition: background-color 0.2s ease, transform 0.2s ease;
    }

    .nav-button:hover:not(:disabled),
    .nav-button:focus-visible:not(:disabled) {
      background: rgba(15, 107, 77, 0.14);
      transform: translateY(-1px);
      outline: none;
    }

    .nav-button:disabled {
      opacity: 0.35;
      cursor: not-allowed;
      transform: none;
    }

    .weekdays,
    .calendar-grid {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 0.35rem;
    }

    .weekdays {
      margin-bottom: 0.45rem;
      color: #6a8175;
      font-size: 0.82rem;
      text-align: center;
    }

    .day-cell {
      border: 0;
      min-height: 2.45rem;
      border-radius: 12px;
      background: transparent;
      color: var(--text);
      transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
    }

    .day-cell:hover:not(:disabled),
    .day-cell:focus-visible:not(:disabled) {
      background: rgba(15, 107, 77, 0.08);
      transform: translateY(-1px);
      outline: none;
    }

    .day-cell.is-selected {
      background: linear-gradient(135deg, #145f9c, #0f6b4d);
      color: #ffffff;
      box-shadow: 0 10px 20px rgba(20, 95, 156, 0.24);
    }

    .day-cell.is-today {
      background: rgba(20, 95, 156, 0.1);
      color: #145f9c;
      font-weight: 700;
    }

    .day-cell.is-outside {
      color: #9aa9a1;
    }

    .day-cell:disabled {
      color: #c1cbc6;
      cursor: not-allowed;
    }

    .date-actions {
      margin-top: 0.75rem;
    }

    .link-button {
      border: 0;
      background: transparent;
      color: var(--primary-2);
      font-weight: 600;
      padding: 0.25rem 0.1rem;
    }
  `]
})
export class DateFieldComponent implements ControlValueAccessor {
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  @Input() placeholder = 'dd/mm/aaaa';
  @Input() min: string | null = null;
  @Input() max: string | null = null;
  @Input() clearable = false;
  @Input() emptyValue = '';

  readonly weekdays = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'];
  value: string | null = null;
  disabled = false;
  isOpen = false;
  visibleMonth = this.startOfMonth(new Date());

  private onChange: (value: string | null) => void = () => {};
  private onTouched: () => void = () => {};

  get displayValue(): string {
    return this.value ? this.formatDate(this.value) : '';
  }

  get monthLabel(): string {
    return this.visibleMonth.toLocaleDateString('pt-BR', {
      month: 'long',
      year: 'numeric'
    });
  }

  get calendarDays(): CalendarDay[] {
    const monthStart = this.startOfMonth(this.visibleMonth);
    const gridStart = new Date(monthStart);
    gridStart.setDate(gridStart.getDate() - gridStart.getDay());

    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(gridStart);
      date.setDate(gridStart.getDate() + index);
      const value = this.toInputValue(date);

      return {
        date: value,
        dayNumber: date.getDate(),
        inCurrentMonth: date.getMonth() === monthStart.getMonth(),
        disabled: !this.isWithinRange(value),
        today: value === this.todayValue()
      };
    });
  }

  writeValue(value: string | null): void {
    this.value = value;
    if (value) {
      this.visibleMonth = this.startOfMonth(this.parseDate(value));
    }
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.isOpen = false;
    }
  }

  toggle(): void {
    if (this.disabled) {
      return;
    }

    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.visibleMonth = this.startOfMonth(this.parseDate(this.value || this.todayValue()));
    }
  }

  previousMonth(): void {
    if (!this.canGoPreviousMonth()) {
      return;
    }

    this.visibleMonth = new Date(this.visibleMonth.getFullYear(), this.visibleMonth.getMonth() - 1, 1);
  }

  nextMonth(): void {
    if (!this.canGoNextMonth()) {
      return;
    }

    this.visibleMonth = new Date(this.visibleMonth.getFullYear(), this.visibleMonth.getMonth() + 1, 1);
  }

  canGoPreviousMonth(): boolean {
    if (!this.min) {
      return true;
    }

    const currentMonthStart = this.startOfMonth(this.visibleMonth);
    const minMonthStart = this.startOfMonth(this.parseDate(this.min));
    return currentMonthStart > minMonthStart;
  }

  canGoNextMonth(): boolean {
    if (!this.max) {
      return true;
    }

    const currentMonthStart = this.startOfMonth(this.visibleMonth);
    const maxMonthStart = this.startOfMonth(this.parseDate(this.max));
    return currentMonthStart < maxMonthStart;
  }

  selectDay(value: string): void {
    if (!this.isWithinRange(value)) {
      return;
    }

    this.value = value;
    this.onChange(value);
    this.onTouched();
    this.isOpen = false;
  }

  clear(): void {
    this.value = null;
    this.onChange(this.emptyValue);
    this.onTouched();
    this.isOpen = false;
  }

  goToToday(): void {
    const today = this.todayValue();
    this.visibleMonth = this.startOfMonth(this.parseDate(today));
    if (this.isWithinRange(today)) {
      this.selectDay(today);
    }
  }

  markAsTouched(): void {
    this.onTouched();
  }

  onTriggerKeydown(event: KeyboardEvent): void {
    if (this.disabled) {
      return;
    }

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.toggle();
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (!this.isOpen) {
        this.toggle();
      }
    }
  }

  onDayKeydown(event: KeyboardEvent, currentDate: string): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.isOpen = false;
      return;
    }

    const offsetMap: Record<string, number> = {
      ArrowLeft: -1,
      ArrowRight: 1,
      ArrowUp: -7,
      ArrowDown: 7
    };

    const offset = offsetMap[event.key];
    if (offset === undefined) {
      return;
    }

    event.preventDefault();
    const nextDate = this.shiftDate(currentDate, offset);
    if (!this.isWithinRange(nextDate)) {
      return;
    }

    this.visibleMonth = this.startOfMonth(this.parseDate(nextDate));
    this.selectDay(nextDate);
  }

  @HostListener('document:click', ['$event.target'])
  onDocumentClick(target: EventTarget | null): void {
    if (!target || !this.isOpen) {
      return;
    }

    if (!this.elementRef.nativeElement.contains(target as Node)) {
      this.isOpen = false;
    }
  }

  private formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }

  private isWithinRange(value: string): boolean {
    if (this.min && value < this.min) {
      return false;
    }

    if (this.max && value > this.max) {
      return false;
    }

    return true;
  }

  private shiftDate(value: string, days: number): string {
    const date = this.parseDate(value);
    date.setDate(date.getDate() + days);
    return this.toInputValue(date);
  }

  private todayValue(): string {
    return this.toInputValue(new Date());
  }

  private toInputValue(date: Date): string {
    const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return localDate.toISOString().split('T')[0];
  }

  private parseDate(value: string): Date {
    return new Date(`${value}T00:00:00`);
  }

  private startOfMonth(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), 1);
  }
}
