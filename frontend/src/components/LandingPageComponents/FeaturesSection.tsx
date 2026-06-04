const features = [
  {
    title: "Multimodalno načrtovanje poti",
    description:
      "ŠibaM izračuna pot po Mariboru z načini premikanja, ki jih lahko uporabiš samostojno ali v kombinaciji.",
    items: [
      "peš odseki",
      "Mbajk kolesa",
      "avtobusne povezave",
      "kombinirane poti med različnimi načini",
    ],
  },
  {
    title: "Pametna izbira časa",
    description:
      "Pot lahko načrtuješ glede na čas odhoda ali glede na uro, do katere želiš prispeti na cilj.",
    items: [
      "odhod ob izbrani uri",
      "prihod do izbrane ure",
      "izbira datuma v naslednjih 7 dneh",
      "ločena izbira avtobusa in kolesa",
    ],
  },
  {
    title: "Interaktivni zemljevid",
    description:
      "Google Maps zemljevid prikaže izbrane lokacije, pot in ključne točke poti neposredno na zemljevidu.",
    items: [
      "markerji začetka in cilja",
      "trenutna lokacija uporabnika",
      "približevanje, oddaljevanje in lociranje",
      "barvno ločeni načini premikanja",
    ],
  },
  {
    title: "Navodila in sledenje",
    description:
      "Po izračunu poti lahko izbiraš med alternativami, pregledaš navodila in začneš aktivno sledenje.",
    items: [
      "kartice z alternativami poti",
      "trajanje in načini prevoza za vsako pot",
      "navodila po korakih",
      "prikaz aktualnega koraka med sledenjem",
    ],
  },
  {
    title: "Shranjene lokacije in poti",
    description:
      "Prijavljeni uporabniki lahko shranijo pomembne lokacije in izračunane poti ter jih kasneje ponovno uporabijo.",
    items: [
      "lokacije z imenom, barvo in ikono",
      "shranjevanje izračunanih poti",
      "prikaz shranjenih vsebin v profilu",
      "brisanje shranjenih lokacij in poti",
    ],
  },
  {
    title: "Podatki za mestno mobilnost",
    description:
      "Pri odsekih poti aplikacija prikaže podatke za Mbajk in avtobus.",
    items: [
      "Mbajk postaje za prevzem in oddajo",
      "prosta kolesa in stojala, ko so podana",
      "avtobusne linije, smeri in odhodi",
      "napovedi razpoložljivosti koles, stojal in zamud",
    ],
  },
];

export const FeaturesSection = () => {
  return (
    <section className='bg-background px-6 py-20 text-foreground lg:px-16 lg:py-28'>
      <div className='mx-auto flex max-w-6xl flex-col gap-16'>
        <div className='max-w-2xl'>
          <span className='text-sm font-semibold uppercase tracking-wide text-red-700'>
            Funkcionalnosti
          </span>
          <h2 className='mt-3 text-4xl font-bold leading-tight lg:text-5xl'>
            Načrtovanje poti, prilagojeno tvojemu načinu premikanja.
          </h2>
        </div>

        <div className='flex flex-col gap-20 lg:gap-28'>
          {features.map((feature, index) => {
            const isReversed = index % 2 === 1;

            return (
              <article
                key={feature.title}
                className={`grid items-center gap-8 lg:grid-cols-2 ${
                  isReversed ? "lg:[&>*:first-child]:order-2" : ""
                }`}>
                <div>
                  <span className='text-sm font-semibold text-red-700'>
                    0{index + 1}
                  </span>
                  <h3 className='mt-3 text-2xl font-bold lg:text-3xl'>
                    {feature.title}
                  </h3>
                  <p className='mt-4 text-lg leading-8 text-muted-foreground'>
                    {feature.description}
                  </p>
                </div>

                <div className='rounded-lg border border-border bg-card p-6 text-card-foreground shadow-sm'>
                  <ul className='space-y-4'>
                    {feature.items.map((item) => (
                      <li
                        key={item}
                        className='flex items-start gap-3 text-base text-muted-foreground'>
                        <span className='mt-2 h-2 w-2 shrink-0 rounded-full bg-red-700' />
                        <span>{item}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
};
