import { EspacoTipo } from './models';

export const DEFAULT_ESPACO_TIPOS: EspacoTipo[] = ['QUADRA', 'QUIOSQUE'];
export const CREATE_NEW_ESPACO_TIPO = '__NOVO_TIPO__';

interface CollectEspacoTiposOptions {
  includeDefaults?: boolean;
}

export function normalizeEspacoTipo(value: string | null | undefined): EspacoTipo {
  return (value ?? '').trim().replace(/\s+/g, ' ').toLocaleUpperCase('pt-BR');
}

export function formatEspacoTipoLabel(value: string | null | undefined): string {
  const normalized = normalizeEspacoTipo(value);
  if (!normalized) {
    return 'Não informado';
  }

  return normalized
    .toLocaleLowerCase('pt-BR')
    .split(/[\s_-]+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toLocaleUpperCase('pt-BR') + word.slice(1))
    .join(' ');
}

export function collectEspacoTipos(
  values: Array<string | null | undefined>,
  options: CollectEspacoTiposOptions = {}
): EspacoTipo[] {
  const tipos = new Set<EspacoTipo>();

  if (options.includeDefaults) {
    for (const tipo of DEFAULT_ESPACO_TIPOS) {
      tipos.add(tipo);
    }
  }

  for (const value of values) {
    const normalized = normalizeEspacoTipo(value);
    if (normalized) {
      tipos.add(normalized);
    }
  }

  return [...tipos].sort(compareEspacoTipos);
}

function compareEspacoTipos(left: EspacoTipo, right: EspacoTipo): number {
  const leftDefaultIndex = DEFAULT_ESPACO_TIPOS.indexOf(left);
  const rightDefaultIndex = DEFAULT_ESPACO_TIPOS.indexOf(right);

  if (leftDefaultIndex !== -1 || rightDefaultIndex !== -1) {
    if (leftDefaultIndex === -1) {
      return 1;
    }

    if (rightDefaultIndex === -1) {
      return -1;
    }

    return leftDefaultIndex - rightDefaultIndex;
  }

  return formatEspacoTipoLabel(left).localeCompare(formatEspacoTipoLabel(right), 'pt-BR');
}
