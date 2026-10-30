import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, Observable, catchError, filter, of, switchMap } from 'rxjs';
import { AdminApi } from '../../../core/admin/admin-api.service';
import { CONTENT_STATUS, DATE_PROVIDERS, fromIsoDate, toIsoDate } from '../../../core/admin/admin-labels';
import { EditorialDetail, EditorialRequest, EtapaRevision } from '../../../core/admin/admin.models';
import { applyFieldErrors, toApiError } from '../../../core/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { TipoContenido } from '../../../core/api.models';
import { TIPO_CONTENIDO, accesoLabel } from '../../../core/content-labels';
import { ContentService } from '../../../core/content.service';
import { ConfirmOptions, confirmAction } from '../../../shared/confirm-dialog/confirm-dialog';
import { StateMessage } from '../../../shared/state-message/state-message';
import { StatusBadge } from '../../../shared/status-badge/status-badge';
import { noBlank } from '../../../shared/validators';
import { BlockEditor } from './blocks/block-editor';
import { SubmitReviewData, SubmitReviewDialog, SubmitReviewResult } from './submit-review-dialog';

type RecomendacionControls = {
  texto: FormControl<string>;
  fuerza: FormControl<string>;
  nivelEvidencia: FormControl<string>;
  referencia: FormControl<string>;
};

const MAX_KEY_POINTS = 20;
const MAX_RECOMMENDATIONS = 60;
const URL_PATTERN = /^https?:\/\/[^\s/$.?#][^\s]*$/i;
const DOI_PATTERN = /^10\.\d{4,9}\/\S+$/;
const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;

@Component({
  selector: 'app-content-editor-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatAutocompleteModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    BlockEditor,
    StateMessage,
    StatusBadge,
  ],
  providers: [DATE_PROVIDERS],
  templateUrl: './content-editor-page.html',
  styleUrl: './content-editor-page.scss',
})
export class ContentEditorPage {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);

  /** Parámetro `:id`; ausente al crear. */
  readonly id = input<string>();

  protected readonly status = CONTENT_STATUS;
  protected readonly maxKeyPoints = MAX_KEY_POINTS;
  protected readonly maxRecommendations = MAX_RECOMMENDATIONS;
  protected readonly saving = signal(false);
  protected readonly busy = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly resource = rxResource({
    params: () => this.id(),
    stream: ({ params }) => (params ? this.api.content(params) : of(null)),
  });
  protected readonly detail = linkedSignal(() => this.resource.value() ?? null);
  protected readonly creating = computed(() => !this.id());
  protected readonly canEdit = computed(() => this.creating() || !!this.detail()?.acciones.editar);
  protected readonly loadError = computed(() => (this.resource.error() ? toApiError(this.resource.error()) : null));

  protected readonly areas = rxResource({ stream: () => this.api.areas() });
  protected readonly candidates = rxResource({
    params: () => this.auth.can('content.create', 'content.assign'),
    stream: ({ params }) => (params ? this.api.candidates() : of({ autores: [], revisores: [] })),
  });

  protected readonly form = this.fb.group({
    titulo: ['', [Validators.required, noBlank, Validators.maxLength(200)]],
    slug: ['', [Validators.maxLength(220), Validators.pattern(SLUG_PATTERN)]],
    areaMedica: ['', Validators.maxLength(100)],
    tiempoLecturaMinutos: [null as number | null, [Validators.min(1), Validators.max(240)]],
    objetivoAprendizaje: ['', Validators.maxLength(2000)],
    resumen: ['', Validators.maxLength(10000)],
    puntosClave: this.fb.array<FormControl<string>>([]),
    aplicacionClinica: ['', Validators.maxLength(4000)],
    fuenteTitulo: ['', Validators.maxLength(300)],
    fuenteAutores: ['', Validators.maxLength(1000)],
    fuenteUrl: ['', [Validators.maxLength(2000), Validators.pattern(URL_PATTERN)]],
    fuenteDoi: ['', [Validators.maxLength(150), Validators.pattern(DOI_PATTERN)]],
    fechaPublicacionFuente: [null as Date | null],
    fechaRevision: [null as Date | null],
    // Guía de práctica clínica
    institucionEmisora: ['', Validators.maxLength(200)],
    fuenteEdicion: ['', Validators.maxLength(100)],
    poblacionObjetivo: ['', Validators.maxLength(4000)],
    contextoAtencion: ['', Validators.maxLength(4000)],
    alcanceClinico: ['', Validators.maxLength(4000)],
    vigenteHasta: [null as Date | null],
    proximaRevision: [null as Date | null],
    recomendaciones: this.fb.array<FormGroup<RecomendacionControls>>([]),
    // Recurso de estudio
    procedencia: ['', Validators.maxLength(4000)],
    titularDerechos: ['', Validators.maxLength(200)],
    licenciaUso: ['', Validators.maxLength(200)],
    autorizacionPublicacion: [false],
    autorizacionReferencia: ['', Validators.maxLength(2000)],
  });

  /** Tipo elegido al crear; al editar manda el del contenido. */
  protected readonly tipoNuevo = signal<TipoContenido>('ARTICULO');
  protected readonly tipo = computed(() => this.detail()?.tipo ?? this.tipoNuevo());
  protected readonly tipos = TIPO_CONTENIDO;
  protected readonly tipoOpciones = (Object.keys(TIPO_CONTENIDO) as TipoContenido[]).filter(
    (t) => this.auth.can(t === 'ARTICULO' ? 'content.create' : 'guides.create'),
  );
  private readonly contentService = inject(ContentService);
  protected readonly tiers = rxResource({ stream: () => this.contentService.tiers() });
  protected accTier: number | null = null;
  protected accMotivo = '';

  /** Responsables elegidos por el editor (sólo con `asignar`). */
  protected readonly autorId = linkedSignal(() => this.detail()?.autor?.id ?? null);
  protected readonly revisorId = linkedSignal(() => this.detail()?.revisor?.id ?? null);

  protected readonly filteredAreas = signal<string[]>([]);
  protected readonly today = new Date();

  constructor() {
    effect(() => {
      const d = this.detail();
      if (d) {
        this.fill(d);
      } else if (this.creating() && this.points.length === 0) {
        this.addPoint();
      }
      if (this.canEdit()) {
        this.form.enable({ emitEvent: false });
        if (d?.estado === 'PUBLICADO') {
          this.form.controls.slug.disable({ emitEvent: false });
        }
      } else {
        this.form.disable({ emitEvent: false });
      }
    });
    effect(() => this.filteredAreas.set(this.areas.value() ?? []));
    this.form.controls.areaMedica.valueChanges.pipe(takeUntilDestroyed()).subscribe((value) => {
      const term = (value ?? '').trim().toLowerCase();
      const areas = this.areas.value() ?? [];
      // Con un área ya elegida se muestran todas, para poder cambiarla sin borrar el texto.
      const seleccionada = areas.some((area) => area.toLowerCase() === term);
      this.filteredAreas.set(seleccionada ? areas : areas.filter((area) => area.toLowerCase().includes(term)));
    });
  }

  /** Texto del acceso: lo fija el tipo; sólo las guías permiten elegir el tier. */
  protected readonly acceso = computed(() => {
    const d = this.detail();
    if (d) {
      return accesoLabel({
        politica: d.politicaAcceso,
        tierCodigo: d.tierMinimo?.codigo ?? null,
        tierNombre: d.tierMinimo?.nombre ?? null,
      }).label;
    }
    switch (this.tipo()) {
      case 'ARTICULO':
        return 'Gratis con cuenta';
      case 'GUIA_PRACTICA_CLINICA':
        return 'Por tier (Básico por defecto; editable)';
      case 'RECURSO_ESTUDIO':
        return 'Tier Premium';
    }
  });

  protected etapasDisponibles(d: EditorialDetail): { id: EtapaRevision; label: string }[] {
    const etapas: { id: EtapaRevision; label: string }[] = [];
    if (d.acciones.revisionEditorial) {
      etapas.push({ id: 'EDITORIAL', label: 'editorial' });
    }
    if (d.acciones.revisionClinica) {
      etapas.push({ id: 'CLINICA', label: 'clínica' });
    }
    return etapas;
  }

  protected get points(): FormArray<FormControl<string>> {
    return this.form.controls.puntosClave;
  }

  protected addPoint(value = ''): void {
    if (this.points.length < MAX_KEY_POINTS) {
      this.points.push(this.fb.control(value, Validators.maxLength(500)));
    }
  }

  protected get recomendaciones(): FormArray<FormGroup<RecomendacionControls>> {
    return this.form.controls.recomendaciones;
  }

  protected addRecommendation(value?: Partial<Record<keyof RecomendacionControls, string | null>>): void {
    if (this.recomendaciones.length >= MAX_RECOMMENDATIONS) {
      return;
    }
    this.recomendaciones.push(
      this.fb.group({
        texto: [value?.texto ?? '', [Validators.maxLength(1500)]],
        fuerza: [value?.fuerza ?? '', Validators.maxLength(100)],
        nivelEvidencia: [value?.nivelEvidencia ?? '', Validators.maxLength(100)],
        referencia: [value?.referencia ?? '', Validators.maxLength(500)],
      }),
    );
  }

  protected removeRecommendation(index: number): void {
    this.recomendaciones.removeAt(index);
    this.form.markAsDirty();
  }

  protected removePoint(index: number): void {
    this.points.removeAt(index);
    this.form.markAsDirty();
  }

  // ---------------------------------------------------------------------------------------------
  // Guardado
  // ---------------------------------------------------------------------------------------------

  protected save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Revisa los campos marcados.');
      return;
    }
    const request = this.toRequest();
    const current = this.detail();
    this.saving.set(true);
    this.errorMessage.set(null);
    const call = current ? this.api.updateContent(current.id, request) : this.api.createContent(request);
    call.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.snackBar.open(current ? 'Cambios guardados' : 'Borrador creado', undefined, { duration: 3000 });
        if (current) {
          this.detail.set(saved);
        } else {
          void this.router.navigate(['/admin/contenido', saved.id, 'editar'], { replaceUrl: true });
        }
      },
      error: (error: unknown) => this.handleError(error),
    });
  }

  private toRequest(): EditorialRequest {
    const v = this.form.getRawValue();
    const text = (value: string) => value.trim() || null;
    return {
      titulo: v.titulo.trim(),
      slug: text(v.slug),
      areaMedica: text(v.areaMedica),
      objetivoAprendizaje: text(v.objetivoAprendizaje),
      resumen: text(v.resumen),
      puntosClave: v.puntosClave.map((p) => p.trim()).filter((p) => p.length > 0),
      aplicacionClinica: text(v.aplicacionClinica),
      fuenteTitulo: text(v.fuenteTitulo),
      fuenteAutores: text(v.fuenteAutores),
      fuenteUrl: text(v.fuenteUrl),
      fuenteDoi: text(v.fuenteDoi),
      fechaPublicacionFuente: toIsoDate(v.fechaPublicacionFuente),
      fechaRevision: toIsoDate(v.fechaRevision),
      tiempoLecturaMinutos: v.tiempoLecturaMinutos,
      ...(this.creating() ? { tipo: this.tipoNuevo() } : {}),
      institucionEmisora: text(v.institucionEmisora),
      fuenteEdicion: text(v.fuenteEdicion),
      poblacionObjetivo: text(v.poblacionObjetivo),
      contextoAtencion: text(v.contextoAtencion),
      alcanceClinico: text(v.alcanceClinico),
      vigenteHasta: toIsoDate(v.vigenteHasta),
      proximaRevision: toIsoDate(v.proximaRevision),
      recomendaciones: v.recomendaciones
        .filter((r) => r.texto.trim().length > 0)
        .map((r) => ({
          texto: r.texto.trim(),
          fuerza: text(r.fuerza),
          nivelEvidencia: text(r.nivelEvidencia),
          referencia: text(r.referencia),
        })),
      procedencia: text(v.procedencia),
      titularDerechos: text(v.titularDerechos),
      licenciaUso: text(v.licenciaUso),
      autorizacionPublicacion: v.autorizacionPublicacion,
      autorizacionReferencia: text(v.autorizacionReferencia),
      version: this.detail()?.version ?? null,
    };
  }

  private fill(d: EditorialDetail): void {
    this.points.clear({ emitEvent: false });
    (d.puntosClave.length ? d.puntosClave : ['']).forEach((p) => this.addPoint(p));
    this.recomendaciones.clear({ emitEvent: false });
    d.recomendaciones.forEach((r) => this.addRecommendation(r));
    this.accTier = d.tierMinimo?.id ?? null;
    this.form.reset(
      {
        titulo: d.titulo,
        slug: d.slug,
        areaMedica: d.areaMedica ?? '',
        tiempoLecturaMinutos: d.tiempoLecturaMinutos,
        objetivoAprendizaje: d.objetivoAprendizaje ?? '',
        resumen: d.resumen ?? '',
        aplicacionClinica: d.aplicacionClinica ?? '',
        fuenteTitulo: d.fuenteTitulo ?? '',
        fuenteAutores: d.fuenteAutores ?? '',
        fuenteUrl: d.fuenteUrl ?? '',
        fuenteDoi: d.fuenteDoi ?? '',
        fechaPublicacionFuente: fromIsoDate(d.fechaPublicacionFuente),
        fechaRevision: fromIsoDate(d.fechaRevision),
        institucionEmisora: d.institucionEmisora ?? '',
        fuenteEdicion: d.fuenteEdicion ?? '',
        poblacionObjetivo: d.poblacionObjetivo ?? '',
        contextoAtencion: d.contextoAtencion ?? '',
        alcanceClinico: d.alcanceClinico ?? '',
        vigenteHasta: fromIsoDate(d.vigenteHasta),
        proximaRevision: fromIsoDate(d.proximaRevision),
        procedencia: d.procedencia ?? '',
        titularDerechos: d.titularDerechos ?? '',
        licenciaUso: d.licenciaUso ?? '',
        autorizacionPublicacion: d.autorizacionPublicacion,
        autorizacionReferencia: d.autorizacionReferencia ?? '',
      },
      { emitEvent: false },
    );
  }

  // ---------------------------------------------------------------------------------------------
  // Flujo editorial
  // ---------------------------------------------------------------------------------------------

  protected submitForReview(d: EditorialDetail): void {
    const revisores = (this.candidates.value()?.revisores ?? []).filter((p) => p.id !== d.autor?.id);
    this.dialog
      .open<SubmitReviewDialog, SubmitReviewData, SubmitReviewResult>(SubmitReviewDialog, {
        data: { revisores, revisorActual: d.revisor?.id ?? null },
        width: '520px',
        maxWidth: 'calc(100vw - 32px)',
      })
      .afterClosed()
      .pipe(
        filter((result): result is SubmitReviewResult => !!result),
        switchMap((result) => this.track(this.api.submitForReview(d.id, result.revisorId, result.comentario))),
      )
      .subscribe((updated) => this.applied(updated, 'Enviado a revisión'));
  }

  protected approve(d: EditorialDetail, etapa: EtapaRevision): void {
    this.transition(
      {
        title: 'Aprobar revisión',
        message: 'El contenido quedará listo para que un editor lo publique.',
        confirmLabel: 'Aprobar',
        reason: { label: 'Comentario (opcional)', required: false },
      },
      (comentario) => this.api.review(d.id, etapa, 'APROBAR', comentario || null),
      'Revisión aprobada',
    );
  }

  protected requestChanges(d: EditorialDetail, etapa: EtapaRevision): void {
    this.transition(
      {
        title: 'Solicitar cambios',
        message: 'El autor recibirá tu comentario y podrá editar el contenido antes de reenviarlo.',
        confirmLabel: 'Solicitar cambios',
        reason: { label: 'Cambios necesarios', required: true, minLength: 10 },
      },
      (comentario) => this.api.review(d.id, etapa, 'SOLICITAR_CAMBIOS', comentario),
      'Cambios solicitados',
    );
  }

  protected publish(d: EditorialDetail): void {
    this.transition(
      {
        title: 'Publicar contenido',
        message: `«${d.titulo}» será visible para los alumnos ${d.tierMinimo ? `con tier ${d.tierMinimo.nombre} o superior` : 'según su política de acceso'}.`,
        confirmLabel: 'Publicar',
      },
      () => this.api.publish(d.id),
      'Contenido publicado',
    );
  }

  protected archive(d: EditorialDetail): void {
    this.transition(
      {
        title: 'Archivar contenido',
        message:
          'El contenido no se elimina: deja de estar disponible para los alumnos y se conserva con su historial. Puedes restaurarlo más tarde como borrador.',
        confirmLabel: 'Archivar',
        tone: 'danger',
        reason: { label: 'Motivo', required: true },
      },
      (motivo) => this.api.archive(d.id, motivo),
      'Contenido archivado',
    );
  }

  protected restore(d: EditorialDetail): void {
    this.transition(
      {
        title: 'Restaurar como borrador',
        message: 'Volverá a ser editable y deberá pasar de nuevo por revisión antes de publicarse.',
        confirmLabel: 'Restaurar',
        reason: { label: 'Motivo', required: true },
      },
      (motivo) => this.api.restore(d.id, motivo),
      'Contenido restaurado',
    );
  }

  protected saveAccess(d: EditorialDetail): void {
    this.track(this.api.setAccess(d.id, 'POR_TIER', this.accTier, this.accMotivo.trim())).subscribe((updated) => {
      this.accMotivo = '';
      this.applied(updated, 'Acceso actualizado');
    });
  }

  protected blocksChanged(updated: EditorialDetail): void {
    this.detail.set(updated);
    this.snackBar.open('Bloques actualizados', undefined, { duration: 2500 });
  }

  protected saveAssignment(d: EditorialDetail): void {
    this.track(this.api.assign(d.id, this.autorId(), this.revisorId())).subscribe((updated) =>
      this.applied(updated, 'Responsables actualizados'),
    );
  }

  private transition(
    options: ConfirmOptions,
    action: (text: string) => Observable<EditorialDetail>,
    message: string,
  ): void {
    confirmAction(this.dialog, options)
      .pipe(
        filter((text): text is string => text !== undefined),
        switchMap((text) => this.track(action(text))),
      )
      .subscribe((updated) => this.applied(updated, message));
  }

  /** Marca la operación en curso; un error se muestra como aviso y la cadena termina sin valor. */
  private track(call: Observable<EditorialDetail>): Observable<EditorialDetail> {
    this.busy.set(true);
    return call.pipe(
      catchError((error: unknown) => {
        this.handleError(error);
        return EMPTY;
      }),
    );
  }

  private applied(updated: EditorialDetail, message: string): void {
    this.busy.set(false);
    this.detail.set(updated);
    this.snackBar.open(message, undefined, { duration: 3000 });
  }

  private handleError(error: unknown): void {
    this.saving.set(false);
    this.busy.set(false);
    const apiError = toApiError(error);
    const applied = applyFieldErrors(this.form, apiError);
    this.errorMessage.set(apiError.message);
    if (!applied) {
      this.snackBar.open(apiError.message, 'Cerrar', { duration: 6000 });
    }
  }
}
