import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, Input, QueryList, ViewChildren, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SelectFieldOption<T = string | number> {
  value: T;
  label: string;
  hint?: string;
  variant?: 'default' | 'action';
  disabled?: boolean;
}

@Component({
  selector: 'app-select-field',
  standalone: true,
  imports: [CommonModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SelectFieldComponent),
      multi: true
    }
  ],
  template: `
    <div class="select-shell" [class.is-open]="isOpen" [class.is-disabled]="disabled">
      <button
        type="button"
        class="select-trigger"
        [class.is-placeholder]="!selectedOption"
        [class.is-action]="selectedOption?.variant === 'action'"
        [disabled]="disabled"
        [attr.aria-expanded]="isOpen"
        aria-haspopup="listbox"
        (click)="toggle()"
        (blur)="markAsTouched()"
        (keydown)="onTriggerKeydown($event)"
      >
        <span class="select-copy">
          <span class="select-label">{{ selectedOption?.label || placeholder }}</span>
          <span class="select-hint" *ngIf="selectedOption?.hint">{{ selectedOption?.hint }}</span>
        </span>
        <span class="select-icon" aria-hidden="true"></span>
      </button>

      <div class="select-panel" *ngIf="isOpen" role="listbox">
        <button
          #optionButton
          type="button"
          class="select-option"
          *ngFor="let option of options; let index = index"
          [class.is-selected]="isSelected(option)"
          [class.is-action]="option.variant === 'action'"
          [class.is-disabled]="option.disabled"
          [disabled]="option.disabled"
          [attr.aria-selected]="isSelected(option)"
          (click)="select(option)"
          (keydown)="onOptionKeydown($event, index)"
        >
          <span class="select-copy">
            <span class="select-option-label">{{ option.label }}</span>
            <span class="select-hint" *ngIf="option.hint">{{ option.hint }}</span>
          </span>
          <span class="select-option-aside">
            <span class="select-badge" *ngIf="option.variant === 'action' && !isSelected(option)">Novo</span>
            <span class="select-check" *ngIf="isSelected(option)" aria-hidden="true">&larr;</span>
          </span>
        </button>

        <div class="select-empty" *ngIf="options.length === 0">{{ emptyLabel }}</div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .select-shell {
      position: relative;
    }

    .select-trigger {
      width: 100%;
      min-height: 3.5rem;
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

    .select-trigger:hover:not(:disabled),
    .select-shell.is-open .select-trigger {
      border-color: var(--field-border-strong);
      box-shadow: var(--field-shadow);
    }

    .select-shell.is-open .select-trigger {
      transform: translateY(-1px);
    }

    .select-trigger:disabled {
      border-color: rgba(29, 43, 36, 0.08);
      background: linear-gradient(180deg, #f4f7f5 0%, #edf3ef 100%);
      color: #6f857a;
      cursor: not-allowed;
      box-shadow: none;
    }

    .select-trigger.is-placeholder {
      color: #82958a;
    }

    .select-trigger.is-action {
      border-color: rgba(15, 107, 77, 0.28);
      background:
        linear-gradient(135deg, rgba(15, 107, 77, 0.12), rgba(20, 95, 156, 0.08)),
        #ffffff;
      box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.85),
        0 12px 24px rgba(15, 107, 77, 0.08);
    }

    .select-copy {
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 0.18rem;
    }

    .select-label,
    .select-option-label {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .select-trigger.is-action .select-label {
      font-weight: 700;
      color: #123d31;
    }

    .select-hint {
      font-size: 0.78rem;
      line-height: 1.25;
      color: #648073;
      white-space: normal;
    }

    .select-trigger.is-action .select-hint {
      color: #305e4d;
    }

    .select-icon {
      width: 0.8rem;
      height: 0.8rem;
      border-right: 2px solid #47695a;
      border-bottom: 2px solid #47695a;
      transform: rotate(45deg) translateY(-2px);
      flex: 0 0 auto;
      transition: transform 0.2s ease;
    }

    .select-shell.is-open .select-icon {
      transform: rotate(225deg) translateY(-1px);
    }

    .select-panel {
      position: absolute;
      z-index: 30;
      top: calc(100% + 0.45rem);
      left: 0;
      right: 0;
      padding: 0.45rem;
      border-radius: 18px;
      border: 1px solid rgba(29, 43, 36, 0.12);
      background:
        linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 250, 247, 0.98)),
        #ffffff;
      box-shadow: 0 24px 48px rgba(8, 26, 20, 0.16);
      backdrop-filter: blur(12px);
      max-height: 18rem;
      overflow-y: auto;
    }

    .select-option {
      width: 100%;
      border: 1px solid transparent;
      background: transparent;
      color: var(--text);
      border-radius: 12px;
      padding: 0.8rem 0.85rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      text-align: left;
      font: inherit;
      transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
    }

    .select-option:hover:not(:disabled),
    .select-option:focus-visible {
      background: rgba(15, 107, 77, 0.08);
      transform: translateX(2px);
      outline: none;
    }

    .select-option.is-action {
      margin-top: 0.35rem;
      border-style: dashed;
      border-color: rgba(15, 107, 77, 0.22);
      background: linear-gradient(135deg, rgba(15, 107, 77, 0.08), rgba(20, 95, 156, 0.06));
    }

    .select-option.is-action:hover:not(:disabled),
    .select-option.is-action:focus-visible {
      border-color: rgba(15, 107, 77, 0.38);
      background: linear-gradient(135deg, rgba(15, 107, 77, 0.12), rgba(20, 95, 156, 0.1));
    }

    .select-option.is-selected {
      background: linear-gradient(135deg, rgba(20, 95, 156, 0.14), rgba(15, 107, 77, 0.14));
      color: #123d31;
      font-weight: 600;
    }

    .select-option.is-selected.is-action {
      border-color: rgba(15, 107, 77, 0.4);
      background: linear-gradient(135deg, rgba(20, 95, 156, 0.18), rgba(15, 107, 77, 0.2));
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55);
    }

    .select-option.is-disabled {
      color: #8fa096;
    }

    .select-option-aside {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 0.55rem;
      flex: 0 0 auto;
      min-height: 1.25rem;
    }

    .select-badge {
      display: inline-flex;
      align-items: center;
      border-radius: 999px;
      padding: 0.18rem 0.5rem;
      background: rgba(15, 107, 77, 0.12);
      color: #0f6b4d;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.03em;
      text-transform: uppercase;
    }

    .select-check {
      color: var(--primary);
      font-size: 1rem;
      font-weight: 700;
      letter-spacing: 0;
      line-height: 1;
    }

    .select-empty {
      padding: 0.9rem 0.85rem;
      color: var(--muted);
      font-size: 0.92rem;
    }
  `]
})
export class SelectFieldComponent<T = string | number> implements ControlValueAccessor {
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  @ViewChildren('optionButton') private readonly optionButtons?: QueryList<ElementRef<HTMLButtonElement>>;

  @Input() placeholder = 'Selecione';
  @Input() emptyLabel = 'Nenhuma opção disponível';
  @Input() options: Array<SelectFieldOption<T>> = [];
  @Input() uiDisabled = false;

  value: T | null = null;
  private controlDisabled = false;
  isOpen = false;

  private onChange: (value: T | null) => void = () => {};
  private onTouched: () => void = () => {};

  get selectedOption(): SelectFieldOption<T> | undefined {
    return this.options.find((option) => this.compareValues(option.value, this.value));
  }

  get disabled(): boolean {
    return this.controlDisabled || this.uiDisabled;
  }

  writeValue(value: T | null): void {
    this.value = value;
  }

  registerOnChange(fn: (value: T | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.controlDisabled = isDisabled;
    if (isDisabled) {
      this.isOpen = false;
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

    this.isOpen = true;
    queueMicrotask(() => this.focusOption(this.selectedIndexOrFirstEnabled()));
  }

  select(option: SelectFieldOption<T>): void {
    if (option.disabled) {
      return;
    }

    this.value = option.value;
    this.onChange(option.value);
    this.onTouched();
    this.close();
  }

  isSelected(option: SelectFieldOption<T>): boolean {
    return this.compareValues(option.value, this.value);
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
      else {
        this.focusOption(this.selectedIndexOrFirstEnabled());
      }
    }
  }

  onOptionKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.focusNextEnabled(index, 1);
      return;
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.focusNextEnabled(index, -1);
      return;
    }

    if (event.key === 'Home') {
      event.preventDefault();
      this.focusOption(this.firstEnabledIndex());
      return;
    }

    if (event.key === 'End') {
      event.preventDefault();
      this.focusOption(this.lastEnabledIndex());
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

  private selectedIndexOrFirstEnabled(): number {
    const selectedIndex = this.options.findIndex((option) => this.isSelected(option) && !option.disabled);
    return selectedIndex >= 0 ? selectedIndex : this.firstEnabledIndex();
  }

  private firstEnabledIndex(): number {
    return this.options.findIndex((option) => !option.disabled);
  }

  private lastEnabledIndex(): number {
    return this.options.length - 1 - [...this.options].reverse().findIndex((option) => !option.disabled);
  }

  private focusNextEnabled(index: number, direction: 1 | -1): void {
    let nextIndex = index + direction;

    while (nextIndex >= 0 && nextIndex < this.options.length) {
      if (!this.options[nextIndex].disabled) {
        this.focusOption(nextIndex);
        return;
      }
      nextIndex += direction;
    }
  }

  private focusOption(index: number): void {
    if (index < 0) {
      return;
    }

    this.optionButtons?.get(index)?.nativeElement.focus();
  }

  private compareValues(left: T | null, right: T | null): boolean {
    return String(left) === String(right);
  }
}
