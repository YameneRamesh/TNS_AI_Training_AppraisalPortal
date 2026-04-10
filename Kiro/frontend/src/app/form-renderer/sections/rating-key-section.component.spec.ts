import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';
import { RatingKeySectionComponent } from './rating-key-section.component';

describe('RatingKeySectionComponent', () => {
  let component: RatingKeySectionComponent;
  let fixture: ComponentFixture<RatingKeySectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatingKeySectionComponent],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(RatingKeySectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }, 30000);

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should define exactly four rating levels', () => {
    expect(component.ratingLevels.length).toBe(4);
  });

  it('should include all four competency rating levels', () => {
    const levels = component.ratingLevels.map((r) => r.level);
    expect(levels).toContain('Excels');
    expect(levels).toContain('Exceeds');
    expect(levels).toContain('Meets');
    expect(levels).toContain('Developing');
  });

  it('should provide a non-empty description for each rating level', () => {
    for (const rating of component.ratingLevels) {
      expect(rating.description.trim().length).toBeGreaterThan(0);
    }
  });

  it('should render the section title', () => {
    const heading = fixture.nativeElement.querySelector('h2');
    expect(heading?.textContent).toContain('Rating Key');
  });

  it('should render a card for each rating level', () => {
    const cards = fixture.nativeElement.querySelectorAll('mat-card');
    expect(cards.length).toBe(4);
  });

  it('should render Excels description in the DOM', () => {
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Excels');
    expect(html).toContain('Exceeds');
    expect(html).toContain('Meets');
    expect(html).toContain('Developing');
  });
});
