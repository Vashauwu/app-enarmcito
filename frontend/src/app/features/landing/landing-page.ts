import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { APP_NAME } from '../../core/brand';
import { ContentService } from '../../core/content.service';
import { MedicalDisclaimer } from '../../shared/medical-disclaimer/medical-disclaimer';
import { ThemeToggle } from '../../shared/theme-toggle/theme-toggle';

interface InfoItem {
  icon: string;
  title: string;
  text: string;
}

interface Faq {
  question: string;
  answer: string;
}

@Component({
  selector: 'app-landing-page',
  imports: [MatButtonModule, MatCardModule, MatIconModule, RouterLink, MedicalDisclaimer, ThemeToggle],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss',
})
export class LandingPage {
  protected readonly appName = APP_NAME;
  protected readonly year = new Date().getFullYear();
  protected readonly auth = inject(AuthService);
  private readonly contentService = inject(ContentService);

  /** Catálogo público de tiers; si falla, la sección simplemente no se muestra. */
  protected readonly tiers = rxResource({ stream: () => this.contentService.tiers() });

  /** Qué incluye cada tipo de contenido y quién puede verlo. */
  protected readonly contentTypes: InfoItem[] = [
    {
      icon: 'article',
      title: 'Artículos',
      text: 'Lecturas breves y verificables. Gratis con tu cuenta, sin elegir nivel.',
    },
    {
      icon: 'account_tree',
      title: 'Guías de práctica clínica',
      text: 'Recomendaciones convertidas en algoritmos, tablas y puntos de decisión. Según tu nivel de acceso.',
    },
    {
      icon: 'collections_bookmark',
      title: 'Recursos de estudio',
      text: 'Material de repaso con diagramas y comparativas, claramente distinto de una guía oficial. Nivel Premium.',
    },
  ];

  /** Lo que se puede comprobar de cada guía. */
  protected readonly trust: InfoItem[] = [
    {
      icon: 'apartment',
      title: 'Emisor y versión',
      text: 'Quién publicó la guía original y cuál es la edición que estás leyendo.',
    },
    {
      icon: 'menu_book',
      title: 'Fuentes citadas',
      text: 'Referencias con enlace o DOI cuando existen, para ir directo al origen.',
    },
    {
      icon: 'event_available',
      title: 'Fecha de revisión',
      text: 'Cuándo se revisó por última vez. Una guía vencida se marca como «requiere actualización».',
    },
    {
      icon: 'medical_services',
      title: 'Revisión clínica',
      text: 'Un revisor médico distinto del autor valida el contenido antes de publicarlo.',
    },
  ];

  /** Las tres profundidades de un mismo tema. */
  protected readonly levels: InfoItem[] = [
    {
      icon: 'bolt',
      title: 'Vistazo',
      text: 'Objetivo, resumen y puntos clave para situarte en 3 a 5 minutos.',
    },
    {
      icon: 'chrome_reader_mode',
      title: 'Comprensión',
      text: 'El desarrollo completo con la lógica detrás de cada recomendación.',
    },
    {
      icon: 'schema',
      title: 'Repaso visual',
      text: 'Algoritmos y tablas para repasar la decisión sin releer todo el texto.',
    },
  ];

  /** Proceso editorial: coincide con el flujo real de la plataforma. */
  protected readonly process: string[] = [
    'Redacción',
    'Revisión editorial',
    'Revisión clínica',
    'Publicación',
    'Actualización periódica',
  ];

  protected readonly faqs: Faq[] = [
    {
      question: '¿Cuánto cuesta empezar?',
      answer:
        'Crear una cuenta es gratis y da acceso a todos los artículos. Las guías y los recursos de estudio dependen del nivel de acceso de tu cuenta.',
    },
    {
      question: '¿Puedo entrar con mi cuenta de Google?',
      answer:
        'Sí, cuando está disponible. Google solo confirma quién eres: tu nivel de acceso lo define la plataforma, nunca Google. Si ya tienes cuenta con ese correo, inicia sesión con tu contraseña y vincula Google desde Mi perfil.',
    },
    {
      question: '¿De dónde sale la información?',
      answer:
        'Cada contenido cita su fuente y muestra la fecha de la última revisión. Las guías indican además el emisor y la versión.',
    },
    {
      question: '¿Cómo sé si una guía sigue vigente?',
      answer:
        'Las guías tienen fecha de revisión. Cuando vence, se marcan como «requiere actualización» hasta que se revisan de nuevo.',
    },
    {
      question: '¿Puedo usar esto para atender pacientes?',
      answer:
        'No. Es material educativo para estudiar y no sustituye el juicio clínico profesional ni la atención médica.',
    },
  ];
}
