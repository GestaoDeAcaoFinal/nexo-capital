import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { routes } from './app.routes';
import { OperacaoService } from './services/operacao.service';

type Rgb = [number, number, number];

function parseCssColor(value: string): { rgb: Rgb; alpha: number } {
  const channels = value.match(/[\d.]+/g)?.map(Number);
  if (!channels || channels.length < 3) {
    throw new Error(`Cor CSS inválida: ${value}`);
  }

  return {
    rgb: [channels[0], channels[1], channels[2]],
    alpha: channels[3] ?? 1
  };
}

function compositeColor(foreground: ReturnType<typeof parseCssColor>, background: Rgb): Rgb {
  return foreground.rgb.map((channel, index) =>
    channel * foreground.alpha + background[index] * (1 - foreground.alpha)
  ) as Rgb;
}

function relativeLuminance(rgb: Rgb): number {
  const [red, green, blue] = rgb.map(channel => {
    const value = channel / 255;
    return value <= .03928 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4;
  });
  return .2126 * red + .7152 * green + .0722 * blue;
}

function contrastRatio(first: Rgb, second: Rgb): number {
  const lighter = Math.max(relativeLuminance(first), relativeLuminance(second));
  const darker = Math.min(relativeLuminance(first), relativeLuminance(second));
  return (lighter + .05) / (darker + .05);
}

describe('AppComponent', () => {
  beforeEach(async () => {
    const operacaoService = jasmine.createSpyObj<OperacaoService>('OperacaoService', ['resumirCarteira', 'listar']);
    operacaoService.resumirCarteira.and.returnValue(of({
      posicoes: [],
      quantidadeTotal: 0,
      precoMedioCarteira: 0,
      custoTotalCarteira: 0,
      valorAtualCarteira: 0
    }));
    operacaoService.listar.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter(routes),
        { provide: OperacaoService, useValue: operacaoService }
      ]
    }).compileComponents();
  });

  it('cria o shell da aplicação', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('apresenta a identidade institucional Nexo Capital', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    expect(fixture.componentInstance.title).toBe('Nexo Capital');
    expect(host.querySelector('.brand')?.getAttribute('aria-label'))
      .toBe('Nexo Capital — visão geral');
    expect(host.querySelector('.brand__mark')?.textContent?.trim()).toBe('NC');
    expect(host.querySelector('.brand__name')?.textContent).toContain('Nexo Capital');
  });

  it('usa superfícies e raios contidos no sistema institucional', () => {
    const tokens = getComputedStyle(document.documentElement);
    expect(tokens.getPropertyValue('--radius-sm').trim()).toBe('.375rem');
    expect(tokens.getPropertyValue('--radius-lg').trim()).toBe('.625rem');
    expect(tokens.getPropertyValue('--shadow-card').trim())
      .toBe('0 6px 18px rgba(7, 26, 47, .06)');
  });

  it('renderiza a página inicial na saída de rotas', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    await TestBed.inject(Router).navigateByUrl('/');
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Visão consolidada');
  });

  it('renderiza a carteira consolidada na saída de rotas', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    await TestBed.inject(Router).navigateByUrl('/carteira');
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Carteira consolidada');
  });

  it('expõe navegação financeira acessível e ação principal', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const nav = host.querySelector('nav[aria-label="Navegação principal"]');
    const destinations = Array.from(nav?.querySelectorAll('a') ?? [])
      .map(link => link.getAttribute('href'));

    expect(nav).not.toBeNull();
    expect(destinations).toEqual(['/', '/carteira', '/corretoras', '/acoes', '/operacoes']);
    expect(host.querySelector('a[href="/operacoes/nova"]')?.textContent)
      .toContain('Nova operação');
    expect(host.querySelector('main.app-main router-outlet')).not.toBeNull();
  });

  it('empilha barras e ações no breakpoint móvel', () => {
    const mediaRules = Array.from(document.styleSheets)
      .flatMap(sheet => Array.from(sheet.cssRules))
      .filter((rule): rule is CSSMediaRule => rule instanceof CSSMediaRule);
    const mobileRule = mediaRules.find(rule => rule.conditionText === '(max-width: 760px)');

    expect(mobileRule).toBeDefined();
    if (!mobileRule) return;

    const originalMedia = mobileRule.media.mediaText;
    mobileRule.media.mediaText = 'all';

    const pageShell = document.createElement('div');
    pageShell.className = 'page-shell';
    const listToolbar = document.createElement('div');
    listToolbar.className = 'list-toolbar';
    const primaryAction = document.createElement('a');
    primaryAction.className = 'btn-finance-primary';
    listToolbar.appendChild(primaryAction);
    const formActions = document.createElement('div');
    formActions.className = 'form-actions';
    const formAction = document.createElement('button');
    formActions.appendChild(formAction);
    pageShell.append(listToolbar, formActions);
    document.body.appendChild(pageShell);

    try {
      expect(pageShell.getBoundingClientRect().width)
        .toBeCloseTo(Math.min(document.documentElement.clientWidth - 16, 1180), 0);
      expect(getComputedStyle(pageShell).paddingTop).toBe('20px');
      expect(getComputedStyle(listToolbar).flexDirection).toBe('column');
      expect(getComputedStyle(listToolbar).alignItems).toBe('stretch');
      expect(getComputedStyle(primaryAction).width).toBe(getComputedStyle(listToolbar).width);
      expect(getComputedStyle(formActions).flexDirection).toBe('column');
      expect(getComputedStyle(formActions).alignItems).toBe('stretch');
      expect(getComputedStyle(formAction).width).toBe(getComputedStyle(formActions).width);
    } finally {
      pageShell.remove();
      mobileRule.media.mediaText = originalMedia;
    }
  });

  it('moves the complete navigation to its own scrollable row before the tablet width', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const mediaRules = Array.from(document.styleSheets)
      .flatMap(sheet => Array.from(sheet.cssRules))
      .filter((rule): rule is CSSMediaRule => rule instanceof CSSMediaRule);
    const tabletRule = mediaRules.find(rule => rule.conditionText === '(max-width: 980px)');

    expect(tabletRule).toBeDefined();
    if (!tabletRule) return;

    const originalMedia = tabletRule.media.mediaText;
    tabletRule.media.mediaText = 'all';
    const header = host.querySelector<HTMLElement>('.app-header');
    const headerWidth = header?.style.width ?? '';

    try {
      const headerInner = host.querySelector<HTMLElement>('.app-header__inner');
      const nav = host.querySelector<HTMLElement>('.primary-nav');
      const action = host.querySelector<HTMLElement>('.header-action');

      expect(headerInner).not.toBeNull();
      expect(nav).not.toBeNull();
      expect(header).not.toBeNull();
      expect(action).not.toBeNull();

      header!.style.width = '768px';

      expect(getComputedStyle(headerInner!).flexWrap).toBe('wrap');
      expect(getComputedStyle(nav!).order).toBe('3');
      expect(getComputedStyle(nav!).overflowX).toBe('auto');
      expect(getComputedStyle(nav!).flexBasis).toBe('100%');
      expect(header!.scrollWidth).toBeLessThanOrEqual(header!.clientWidth);
      expect(nav!.offsetTop).toBeGreaterThan(action!.offsetTop);
      expect(nav!.clientWidth).toBeLessThanOrEqual(header!.clientWidth);
      expect(Array.from(nav!.querySelectorAll('a')).every(link => {
        const style = getComputedStyle(link);
        return style.flexShrink === '0' && link.scrollWidth <= link.clientWidth;
      })).toBeTrue();

    } finally {
      header!.style.width = headerWidth;
      tabletRule.media.mediaText = originalMedia;
    }
  });

  it('reduz transições, animações e rolagem quando a preferência está ativa', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const mediaRules = Array.from(document.styleSheets)
      .flatMap(sheet => Array.from(sheet.cssRules))
      .filter((rule): rule is CSSMediaRule => rule instanceof CSSMediaRule);
    const reducedMotionRule = mediaRules.find(rule =>
      rule.conditionText === '(prefers-reduced-motion: reduce)' &&
      Array.from(rule.cssRules).some(cssRule =>
        cssRule instanceof CSSStyleRule &&
        cssRule.style.getPropertyValue('scroll-behavior') === 'auto' &&
        cssRule.style.getPropertyValue('animation-duration') !== ''
      )
    );

    expect(reducedMotionRule).toBeDefined();
    if (!reducedMotionRule) return;

    const originalMedia = reducedMotionRule.media.mediaText;
    reducedMotionRule.media.mediaText = 'all';

    const probe = document.createElement('div');
    probe.style.setProperty('animation-duration', '2s');
    probe.style.setProperty('scroll-behavior', 'smooth');
    probe.style.setProperty('transition-duration', '2s');
    document.body.appendChild(probe);

    try {
      const style = getComputedStyle(probe);
      expect(style.scrollBehavior).toBe('auto');
      expect(style.transitionDuration).toBe('1e-05s');
      expect(style.animationDuration).toBe('1e-05s');
    } finally {
      probe.remove();
      reducedMotionRule.media.mediaText = originalMedia;
    }
  });

  it('mantém contraste AA no botão primário', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const action = fixture.nativeElement.querySelector('.btn-finance-primary') as HTMLElement;
    const style = getComputedStyle(action);
    const background = parseCssColor(style.backgroundColor).rgb;
    const foreground = compositeColor(parseCssColor(style.color), background);

    expect(contrastRatio(foreground, background)).toBeGreaterThanOrEqual(4.5);
  });

  it('mantém foco opaco perceptível nos controles Bootstrap sobre fundos claro e escuro', () => {
    const backgrounds: Rgb[] = [[255, 255, 255], [7, 26, 47]];
    const controls = [
      { tagName: 'input', className: 'form-control' },
      { tagName: 'select', className: 'form-select' },
      { tagName: 'input', className: 'form-check-input', type: 'checkbox' }
    ];

    for (const background of backgrounds) {
      for (const controlCase of controls) {
        const surface = document.createElement('div');
        const control = document.createElement(controlCase.tagName) as HTMLInputElement;
        control.className = controlCase.className;
        if (controlCase.type) control.type = controlCase.type;
        surface.style.backgroundColor = `rgb(${background.join(', ')})`;
        surface.style.padding = '1rem';
        surface.appendChild(control);
        document.body.appendChild(surface);
        control.focus();

        try {
          const style = getComputedStyle(control);
          const outlineColor = parseCssColor(style.outlineColor);
          expect(control.matches(':focus-visible')).toBeTrue();
          expect(style.outlineStyle).withContext(controlCase.className).toBe('solid');
          expect(style.outlineWidth).withContext(controlCase.className).toBe('3px');
          expect(outlineColor.alpha).withContext(controlCase.className).toBe(1);
          expect(contrastRatio(outlineColor.rgb, background))
            .withContext(`${controlCase.className} sobre rgb(${background.join(', ')})`)
            .toBeGreaterThanOrEqual(3);
        } finally {
          surface.remove();
        }
      }
    }
  });
});
