import { describe, expect, it, vi } from "vitest";
import { MainMap } from "../../components/MainAppComponents/MainMap";
import {
  fireEvent,
  mariborCenter,
  renderWithTheme,
  routeLegs,
  savedLocation,
  screen,
} from "../frontendPlanFixtures";

describe("zemljevid", () => {
  it("prikaz osnovnega zemljevida", () => {
    renderWithTheme(
      <MainMap
        center={mariborCenter}
        zoom={13}
        onMapLocationSave={vi.fn()}
      />,
    );

    expect(screen.getByTestId("mock-map")).toHaveAttribute(
      "data-center",
      JSON.stringify(mariborCenter),
    );
  });

  it("prikaz trenutne lokacije uporabnika", () => {
    renderWithTheme(
      <MainMap
        center={mariborCenter}
        zoom={13}
        userLocationPosition={mariborCenter}
        onMapLocationSave={vi.fn()}
      />,
    );

    expect(screen.getByTestId("mock-marker")).toHaveAttribute(
      "data-position",
      JSON.stringify(mariborCenter),
    );
  });

  it("prikaz shranjenih lokacij", () => {
    renderWithTheme(
      <MainMap
        center={mariborCenter}
        zoom={13}
        savedLocations={[savedLocation]}
        onMapLocationSave={vi.fn()}
      />,
    );

    expect(screen.getByText(savedLocation.name)).toBeInTheDocument();
  });

  it("prikaz izracunane poti", () => {
    renderWithTheme(
      <MainMap
        center={mariborCenter}
        zoom={13}
        legs={routeLegs}
        onMapLocationSave={vi.fn()}
      />,
    );

    expect(screen.getAllByTestId("mock-marker").length).toBeGreaterThan(1);
    expect(screen.getByAltText("Avtobusna postaja")).toBeInTheDocument();
  });

  it("prikaz prestopnih ikon za avtobus in MBajk", () => {
    renderWithTheme(
      <MainMap
        center={mariborCenter}
        zoom={13}
        legs={routeLegs}
        onMapLocationSave={vi.fn()}
      />,
    );

    expect(screen.getByAltText("Avtobusna postaja")).toBeInTheDocument();
    expect(screen.getByAltText("Bajk postaja za prevzem")).toBeInTheDocument();
    expect(screen.getByAltText("Bajk postaja za oddajo")).toBeInTheDocument();
  });

  it("klik na markerje in polyline odseke", () => {
    const onBusIconClick = vi.fn();
    renderWithTheme(
      <MainMap
        center={mariborCenter}
        zoom={13}
        legs={routeLegs}
        onBusIconClick={onBusIconClick}
        onMapLocationSave={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByAltText("Avtobusna postaja"));
    expect(onBusIconClick).toHaveBeenCalledWith(
      routeLegs[1],
      expect.objectContaining({ lat: expect.any(Number), lng: expect.any(Number) }),
      routeLegs[0],
    );
  });

  it("desni klik za ustvarjanje nove shranjene lokacije", () => {
    const onMapContextSelect = vi.fn();
    renderWithTheme(
      <MainMap
        center={mariborCenter}
        zoom={13}
        onMapContextSelect={onMapContextSelect}
        onMapLocationSave={vi.fn()}
      />,
    );

    fireEvent.contextMenu(screen.getByTestId("mock-map"));
    expect(onMapContextSelect).toHaveBeenCalledWith({ lat: 46.56, lng: 15.66 });
  });

  it("brisanje shranjene lokacije prek zemljevida", () => {
    const onSavedLocationDelete = vi.fn();
    renderWithTheme(
      <MainMap
        center={mariborCenter}
        zoom={13}
        savedLocations={[savedLocation]}
        onSavedLocationDelete={onSavedLocationDelete}
        onMapLocationSave={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByText(savedLocation.name));
    fireEvent.click(screen.getByRole("button", { name: "Izbriši" }));
    expect(onSavedLocationDelete).toHaveBeenCalledWith(savedLocation.id);
  });
});
