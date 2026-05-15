import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, Input, QueryList, ViewChildren, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

interface TimeOption {
  value: string;
  hour: string;
  minute: string;
}

@Component({
  selector: 'app-time-field',
  standalone: true,
  imports: [CommonModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeFieldComponent),
      multi: true
    }
  ],
  template: `
    <div class="time-shell" [class.is-open]="isOpen" [class.is-disabled]="disabled">
      <button
        type="button"
        class="time-trigger"
        [class.is-placeholder]="!value"
        [disabled]="disabled"
        [attr.id]="inputId || null"
        [attr.aria-expanded]="isOpen"
        [attr.title]="displayValue || null"
        aria-haspopup="dialog"
        (click)="toggle()"
        (blur)="markAsTouched()"
        (keydown)="onTriggerKeydown($event)"
      >
        <span>{{ displayValue || placeholder }}</span>
        <span class="time-icon" aria-hidden="true"></span>
      </button>

      <div
        class="time-panel"
        [class.is-hour-only]="usesHourOnlySelector"
        *ngIf="isOpen"
        role="dialog"
        aria-label="Selecionar horário"
        (keydown.escape)="close()"
      >
        <div class="time-columns" [class.is-hour-only]="usesHourOnlySelector">
          <section class="time-column">
            <header class="time-column-head">Hora</header>
            <div class="time-option-list">
              <button
                #hourButton
                type="button"
                class="time-option"
                *ngFor="let hour of hourOptions; let index = index"
                [class.is-selected]="hour === draftHour"
                (click)="selectHour(hour)"
                (keydown)="onHourKeydown($event, index)"
              >
                {{ hour }}
              </button>
            </div>
          </section>

          <section class="time-column" *ngIf="!usesHourOnlySelector">
            <header class="time-column-head">Min</header>
            <div class="time-option-list">
              <button
                #minuteButton
                type="button"
                class="time-option"
                *ngFor="let minute of minuteOptions; let index = index"
                [class.is-selected]="minute === draftMinute"
                (click)="selectMinute(minute)"
                (keydown)="onMinuteKeydown($event, index)"
              >
                {{ minute }}
              </button>
            </div>
          </section>
        </div>

        <div class="time-actions" *ngIf="clearable && value">
          <button type="button" class="time-link" (click)="clear()">Limpar</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .time-shell {
      position: relative;
    }

    .time-trigger {
      width: 100%;
      min-height: var(--field-min-height, 3.5rem);
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
        0 0 0 1px rgba(36, 73, 61, 0.05),
        0 1px 0 rgba(255, 255, 255, 0.5);
      transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
      font: inherit;
      text-align: left;
    }

    .time-trigger:hover:not(:disabled),
    .time-shell.is-open .time-trigger {
      border-color: var(--field-border-strong);
      box-shadow: var(--field-shadow);
    }

    .time-shell.is-open .time-trigger {
      transform: translateY(-1px);
    }

    .time-trigger:disabled {
      border-color: rgba(29, 43, 36, 0.08);
      background: linear-gradient(180deg, #f4f7f5 0%, #edf3ef 100%);
      color: #6f857a;
      cursor: not-allowed;
      box-shadow: none;
    }

    .time-trigger.is-placeholder {
      color: #82958a;
    }

    .time-icon {
      position: relative;
      width: 1.15rem;
      height: 1.15rem;
      border: 2px solid #264b3d;
      border-radius: 999px;
      flex: 0 0 auto;
    }

    .time-icon::before,
    .time-icon::after {
      content: "";
      position: absolute;
      left: 50%;
      top: 50%;
      background: #264b3d;
      border-radius: 999px;
      transform-origin: bottom center;
    }

    .time-icon::before {
      width: 2px;
      height: 0.32rem;
      transform: translate(-50%, -95%);
    }

    .time-icon::after {
      width: 2px;
      height: 0.42rem;
      transform: translate(-5%, -75%) rotate(55deg);
    }

    .time-panel {
      position: absolute;
      z-index: 30;
      top: calc(100% + 0.45rem);
      left: 0;
      width: min(19rem, 100vw - 2rem);
      padding: 0.6rem;
      border-radius: 22px;
      border: 1px solid rgba(29, 43, 36, 0.12);
      background:
        linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 250, 247, 0.98)),
        #ffffff;
      box-shadow: 0 24px 48px rgba(8, 26, 20, 0.18);
      backdrop-filter: blur(12px);
    }

    .time-panel.is-hour-only {
      width: min(10rem, 100vw - 2rem);
    }

    .time-columns {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.55rem;
    }

    .time-columns.is-hour-only {
      grid-template-columns: minmax(0, 1fr);
    }

    .time-column {
      min-width: 0;
      border-radius: 16px;
      border: 1px solid rgba(20, 95, 156, 0.1);
      background: rgba(255, 255, 255, 0.68);
      overflow: hidden;
    }

    .time-column-head {
      padding: 0.68rem 0.85rem 0.55rem;
      font-size: 0.78rem;
      font-weight: 700;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      color: #2a684f;
      background: rgba(15, 107, 77, 0.05);
    }

    .time-option-list {
      max-height: 14rem;
      overflow-y: auto;
      padding: 0.32rem;
      display: grid;
      gap: 0.18rem;
    }

    .time-option {
      width: 100%;
      min-height: 2.55rem;
      border: 0;
      border-radius: 12px;
      background: transparent;
      color: var(--text);
      font: inherit;
      text-align: center;
      transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
    }

    .time-option:hover,
    .time-option:focus-visible {
      background: rgba(15, 107, 77, 0.08);
      transform: translateY(-1px);
      outline: none;
    }

    .time-option.is-selected {
      background: linear-gradient(135deg, #145f9c, #0f6b4d);
      color: #ffffff;
      box-shadow: 0 10px 20px rgba(20, 95, 156, 0.2);
      font-weight: 700;
    }

    .time-actions {
      display: flex;
      justify-content: flex-end;
      padding-top: 0.55rem;
    }

    .time-link {
      border: 0;
      background: transparent;
      color: var(--primary-2);
      font-weight: 600;
      padding: 0.25rem 0.1rem;
      font: inherit;
    }
  `]
})
export class TimeFieldComponent implements ControlValueAccessor {
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  @ViewChildren('hourButton') private readonly hourButtons?: QueryList<ElementRef<HTMLButtonElement>>;
  @ViewChildren('minuteButton') private readonly minuteButtons?: QueryList<ElementRef<HTMLButtonElement>>;

  @Input() placeholder = 'hh:mm';
  @Input() inputId: string | null = null;
  @Input() min: string | null = null;
  @Input() max: string | null = null;
  @Input() minuteStep = 60;
  @Input() clearable = false;
  @Input() emptyValue = '';

  value: string | null = null;
  isOpen = false;
  draftHour: string | null = null;
  draftMinute: string | null = null;
  activeColumn: 'hour' | 'minute' = 'hour';

  private controlDisabled = false;
  private _uiDisabled = false;

  private onChange: (value: string | null) => void = () => {};
  private onTouched: () => void = () => {};

  get displayValue(): string {
    return this.value ? this.normalizeTime(this.value) : '';
  }

  get validTimes(): TimeOption[] {
    const options: TimeOption[] = [];
    const step = this.safeMinuteStep();

    for (let totalMinutes = 0; totalMinutes < 24 * 60; totalMinutes += step) {
      const hour = Math.floor(totalMinutes / 60)
        .toString()
        .padStart(2, '0');
      const minute = (totalMinutes % 60).toString().padStart(2, '0');
      const value = `${hour}:${minute}`;

      if (!this.isWithinRange(value)) {
        continue;
      }

      options.push({ value, hour, minute });
    }

    return options;
  }

  get hourOptions(): string[] {
    return [...new Set(this.validTimes.map((option) => option.hour))];
  }

  get minuteOptions(): string[] {
    if (!this.draftHour) {
      return [];
    }

    return this.validTimes
      .filter((option) => option.hour === this.draftHour)
      .map((option) => option.minute);
  }

  get distinctMinuteOptions(): string[] {
    return [...new Set(this.validTimes.map((option) => option.minute))];
  }

  get usesHourOnlySelector(): boolean {
    return this.distinctMinuteOptions.length === 1;
  }

  get disabled(): boolean {
    return this.controlDisabled || this._uiDisabled;
  }

  @Input()
  set uiDisabled(value: boolean) {
    this._uiDisabled = value;
    if (this.disabled) {
      this.close();
    }
  }

  get uiDisabled(): boolean {
    return this._uiDisabled;
  }

  writeValue(value: string | null): void {
    this.value = this.normalizeTime(value);
    this.syncDraftToValue();
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.controlDisabled = isDisabled;
    if (isDisabled) {
      this.close();
    }
  }

  toggle(): void {
    if (this.disabled) {
      return;
    }

    if (this.isOpen) {
      this.close();
      return;
    }

    this.syncDraftToValue();
    this.isOpen = true;
    this.activeColumn = this.usesHourOnlySelector ? 'hour' : (this.draftMinute ? 'minute' : 'hour');
    queueMicrotask(() => this.focusActiveColumn());
  }

  selectHour(hour: string): void {
    this.draftHour = hour;

    const availableMinutes = this.minuteOptions;
    if (availableMinutes.length === 0) {
      return;
    }

    if (!this.draftMinute || !availableMinutes.includes(this.draftMinute)) {
      this.draftMinute = availableMinutes[0];
    }

    this.activeColumn = availableMinutes.length === 1 ? 'hour' : 'minute';
    this.commitSelection(availableMinutes.length === 1);

    if (availableMinutes.length > 1) {
      queueMicrotask(() => this.focusMinute(this.selectedMinuteIndexOrFirst()));
    }
  }

  selectMinute(minute: string): void {
    if (!this.draftHour) {
      return;
    }

    this.draftMinute = minute;
    this.activeColumn = 'minute';
    this.commitSelection(true);
  }

  clear(): void {
    this.value = null;
    this.onChange(this.emptyValue);
    this.onTouched();
    this.close();
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
      else if (!this.usesHourOnlySelector && this.activeColumn === 'minute' && this.minuteOptions.length > 0) {
        this.focusMinute(this.selectedMinuteIndexOrFirst());
      }
      else {
        this.focusHour(this.selectedHourIndexOrFirst());
      }
      return;
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      if (!this.isOpen) {
        this.toggle();
      }
      else if (!this.usesHourOnlySelector && this.minuteOptions.length > 0) {
        this.activeColumn = 'minute';
        this.focusMinute(this.selectedMinuteIndexOrFirst());
      }
      else {
        this.focusHour(this.selectedHourIndexOrFirst());
      }
    }
  }

  onHourKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.focusHour(this.clampIndex(index + 1, this.hourOptions.length));
      return;
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.focusHour(this.clampIndex(index - 1, this.hourOptions.length));
      return;
    }

    if (event.key === 'Home') {
      event.preventDefault();
      this.focusHour(0);
      return;
    }

    if (event.key === 'End') {
      event.preventDefault();
      this.focusHour(this.hourOptions.length - 1);
      return;
    }

    if (event.key === 'ArrowRight' && !this.usesHourOnlySelector && this.minuteOptions.length > 0) {
      event.preventDefault();
      this.activeColumn = 'minute';
      this.focusMinute(this.selectedMinuteIndexOrFirst());
    }
  }

  onMinuteKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.focusMinute(this.clampIndex(index + 1, this.minuteOptions.length));
      return;
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.focusMinute(this.clampIndex(index - 1, this.minuteOptions.length));
      return;
    }

    if (event.key === 'Home') {
      event.preventDefault();
      this.focusMinute(0);
      return;
    }

    if (event.key === 'End') {
      event.preventDefault();
      this.focusMinute(this.minuteOptions.length - 1);
      return;
    }

    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      this.activeColumn = 'hour';
      this.focusHour(this.selectedHourIndexOrFirst());
    }
  }

  @HostListener('document:click', ['$event.target'])
  onDocumentClick(target: EventTarget | null): void {
    if (!target || !this.isOpen) {
      return;
    }

    if (!this.elementRef.nativeElement.contains(target as Node)) {
      this.close();
    }
  }

  private close(): void {
    this.isOpen = false;
  }

  private commitSelection(closeAfter: boolean): void {
    if (!this.draftHour || !this.draftMinute) {
      return;
    }

    const nextValue = `${this.draftHour}:${this.draftMinute}`;
    if (!this.isWithinRange(nextValue)) {
      return;
    }

    this.value = nextValue;
    this.onChange(nextValue);
    this.onTouched();

    if (closeAfter) {
      this.close();
    }
  }

  private syncDraftToValue(): void {
    const candidate = this.value && this.isWithinRange(this.value) ? this.value : this.validTimes[0]?.value ?? null;

    if (!candidate) {
      this.draftHour = null;
      this.draftMinute = null;
      return;
    }

    const normalized = this.normalizeTime(candidate);
    const [hour, minute] = normalized.split(':');
    this.draftHour = hour;
    this.draftMinute = minute;
  }

  private focusActiveColumn(): void {
    if (!this.usesHourOnlySelector && this.activeColumn === 'minute' && this.minuteOptions.length > 0) {
      this.focusMinute(this.selectedMinuteIndexOrFirst());
      return;
    }

    this.activeColumn = 'hour';
    this.focusHour(this.selectedHourIndexOrFirst());
  }

  private selectedHourIndexOrFirst(): number {
    if (!this.draftHour) {
      return 0;
    }

    const selectedIndex = this.hourOptions.findIndex((hour) => hour === this.draftHour);
    return selectedIndex >= 0 ? selectedIndex : 0;
  }

  private selectedMinuteIndexOrFirst(): number {
    if (!this.draftMinute) {
      return 0;
    }

    const selectedIndex = this.minuteOptions.findIndex((minute) => minute === this.draftMinute);
    return selectedIndex >= 0 ? selectedIndex : 0;
  }

  private focusHour(index: number): void {
    this.activeColumn = 'hour';
    this.hourButtons?.get(index)?.nativeElement.focus();
  }

  private focusMinute(index: number): void {
    this.activeColumn = 'minute';
    this.minuteButtons?.get(index)?.nativeElement.focus();
  }

  private clampIndex(index: number, length: number): number {
    if (length <= 0) {
      return 0;
    }

    return Math.min(Math.max(index, 0), length - 1);
  }

  private safeMinuteStep(): number {
    const parsed = Number(this.minuteStep);
    if (!Number.isInteger(parsed) || parsed <= 0) {
      return 60;
    }

    return Math.min(parsed, 60);
  }

  private normalizeTime(value: string | null | undefined): string {
    return (value ?? '').slice(0, 5);
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
}
